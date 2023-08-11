/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.JsSuspendArityStoreLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.JsSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.js.JsPlatforms

private fun DeclarationContainerLoweringPass.runOnFilesPostfix(files: Iterable<IrFile>) = files.forEach { runOnFilePostfix(it) }

private fun ClassLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { runOnFilePostfix(it) }

private fun List<Lowering>.toCompilerPhase() =
    map {
        @Suppress("USELESS_CAST")
        it.modulePhase as CompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>, Iterable<IrModuleFragment>>
    }.reduce { acc, lowering -> acc.then(lowering) }

private fun makeJsModulePhase(
    lowering: (JsIrBackendContext) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<JsIrBackendContext, *, *>> = emptySet()
): SameTypeNamedCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>> = makeCustomJsModulePhase(
    op = { context, modules -> lowering(context).lower(modules) },
    name = name,
    description = description,
    prerequisite = prerequisite
)

private fun makeCustomJsModulePhase(
    op: (JsIrBackendContext, IrModuleFragment) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<AbstractNamedCompilerPhase<JsIrBackendContext, *, *>> = emptySet()
): SameTypeNamedCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>> = SameTypeNamedCompilerPhase(
    name = name,
    description = description,
    prerequisite = prerequisite,
    lower = object : SameTypeCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>> {
        override fun invoke(
            phaseConfig: PhaseConfigurationService,
            phaserState: PhaserState<Iterable<IrModuleFragment>>,
            context: JsIrBackendContext,
            input: Iterable<IrModuleFragment>
        ): Iterable<IrModuleFragment> {
            input.forEach { module ->
                op(context, module)
            }
            return input
        }
    },
    actions = setOf(defaultDumper.toMultiModuleAction(), validationAction.toMultiModuleAction()),
)

sealed class Lowering(val name: String) {
    abstract val modulePhase: SameTypeNamedCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>>
}

class DeclarationLowering(
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<JsIrBackendContext, *, *>> = emptySet(),
    private val factory: (JsIrBackendContext) -> DeclarationTransformer
) : Lowering(name) {
    fun declarationTransformer(context: JsIrBackendContext): DeclarationTransformer {
        return factory(context)
    }

    override val modulePhase = makeJsModulePhase(factory, name, description, prerequisite)
}

class BodyLowering(
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<JsIrBackendContext, *, *>> = emptySet(),
    private val factory: (JsIrBackendContext) -> BodyLoweringPass
) : Lowering(name) {
    fun bodyLowering(context: JsIrBackendContext): BodyLoweringPass {
        return factory(context)
    }

    override val modulePhase = makeJsModulePhase(factory, name, description, prerequisite)
}

class ModuleLowering(
    name: String,
    override val modulePhase: SameTypeNamedCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>>
) : Lowering(name)

private fun makeDeclarationTransformerPhase(
    lowering: (JsIrBackendContext) -> DeclarationTransformer,
    name: String,
    description: String,
    prerequisite: Set<Lowering> = emptySet()
) = DeclarationLowering(name, description, prerequisite.map { it.modulePhase }.toSet(), lowering)

private fun makeBodyLoweringPhase(
    lowering: (JsIrBackendContext) -> BodyLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<Lowering> = emptySet()
) = BodyLowering(name, description, prerequisite.map { it.modulePhase }.toSet(), lowering)

fun SameTypeNamedCompilerPhase<JsIrBackendContext, Iterable<IrModuleFragment>>.toModuleLowering() = ModuleLowering(this.name, this)

private val validateIrBeforeLowering = makeCustomJsModulePhase(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
).toModuleLowering()

private val validateIrAfterLowering = makeCustomJsModulePhase(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
).toModuleLowering()

private val collectClassDefaultConstructorsPhase = makeDeclarationTransformerPhase(
    ::CollectClassDefaultConstructorsLowering,
    name = "CollectClassDefaultConstructorsLowering",
    description = "Collect classes default constructors to add it to metadata on code generating phase"
)

private val preventExportOfSyntheticDeclarationsLowering = makeDeclarationTransformerPhase(
    ::ExcludeSyntheticDeclarationsFromExportLowering,
    name = "ExcludeSyntheticDeclarationsFromExportLowering",
    description = "Exclude synthetic declarations which we don't want to export such as `Enum.entries` or `DataClass::componentN`",
)

val scriptRemoveReceiverLowering = makeJsModulePhase(
    ::ScriptRemoveReceiverLowering,
    name = "ScriptRemoveReceiver",
    description = "Remove receivers for declarations in script"
).toModuleLowering()

val createScriptFunctionsPhase = makeJsModulePhase(
    ::CreateScriptFunctionsPhase,
    name = "CreateScriptFunctionsPhase",
    description = "Create functions for initialize and evaluate script"
).toModuleLowering()

private val collectClassIdentifiersLowering = makeJsModulePhase(
    ::JsCollectClassIdentifiersLowering,
    name = "CollectClassIdentifiersLowering",
    description = "Save classId before all the lowerings",
).toModuleLowering()

private val inventNamesForLocalClassesPhase = makeJsModulePhase(
    ::JsInventNamesForLocalClasses,
    name = "InventNamesForLocalClasses",
    description = "Invent names for local classes and anonymous objects",
).toModuleLowering()

private val annotationInstantiationLowering = makeDeclarationTransformerPhase(
    ::JsAnnotationImplementationTransformer,
    name = "AnnotationImplementation",
    description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

private val expectDeclarationsRemovingPhase = makeDeclarationTransformerPhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

private val stringConcatenationLoweringPhase = makeJsModulePhase(
    ::JsStringConcatenationLowering,
    name = "JsStringConcatenationLowering",
    description = "Call toString() for values of some types when concatenating strings"
).toModuleLowering()

private val lateinitNullableFieldsPhase = makeDeclarationTransformerPhase(
    ::NullableFieldsForLateinitCreationLowering,
    name = "LateinitNullableFields",
    description = "Create nullable fields for lateinit properties"
)

private val lateinitDeclarationLoweringPhase = makeDeclarationTransformerPhase(
    ::NullableFieldsDeclarationLowering,
    name = "LateinitDeclarations",
    description = "Reference nullable fields from properties and getters + insert checks"
)

private val lateinitUsageLoweringPhase = makeBodyLoweringPhase(
    ::LateinitUsageLowering,
    name = "LateinitUsage",
    description = "Insert checks for lateinit field references"
)

private val kotlinNothingValueExceptionPhase = makeBodyLoweringPhase(
    ::KotlinNothingValueExceptionLowering,
    name = "KotlinNothingValueException",
    description = "Throw proper exception for calls returning value of type 'kotlin.Nothing'"
)

private val stripTypeAliasDeclarationsPhase = makeDeclarationTransformerPhase(
    { StripTypeAliasDeclarationsLowering() },
    name = "StripTypeAliasDeclarations",
    description = "Strip typealias declarations"
)

private val jsCodeOutliningPhase = makeBodyLoweringPhase(
    ::JsCodeOutliningLowering,
    name = "JsCodeOutliningLowering",
    description = "Outline js() calls where JS code references Kotlin locals"
)

private val arrayConstructorReferencePhase = makeBodyLoweringPhase(
    ::ArrayConstructorReferenceLowering,
    name = "ArrayConstructorReference",
    description = "Transform `::Array` into a lambda"
)

private val arrayConstructorPhase = makeBodyLoweringPhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    description = "Transform `Array(size) { index -> value }` into a loop",
    prerequisite = setOf(arrayConstructorReferencePhase)
)

private val sharedVariablesLoweringPhase = makeBodyLoweringPhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    description = "Box captured mutable variables",
    prerequisite = setOf(lateinitDeclarationLoweringPhase, lateinitUsageLoweringPhase)
)

private val localClassesInInlineLambdasPhase = makeBodyLoweringPhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
    description = "Extract local classes from inline lambdas",
    prerequisite = setOf(inventNamesForLocalClassesPhase)
)

private val localClassesInInlineFunctionsPhase = makeBodyLoweringPhase(
    ::LocalClassesInInlineFunctionsLowering,
    name = "LocalClassesInInlineFunctionsPhase",
    description = "Extract local classes from inline functions",
    prerequisite = setOf(inventNamesForLocalClassesPhase)
)

private val localClassesExtractionFromInlineFunctionsPhase = makeBodyLoweringPhase(
    { context -> LocalClassesExtractionFromInlineFunctionsLowering(context) },
    name = "localClassesExtractionFromInlineFunctionsPhase",
    description = "Move local classes from inline functions into nearest declaration container",
    prerequisite = setOf(localClassesInInlineFunctionsPhase)
)

private val syntheticAccessorLoweringPhase = makeBodyLoweringPhase(
    ::SyntheticAccessorLowering,
    name = "syntheticAccessorLoweringPhase",
    description = "Wrap top level inline function to access through them from inline functions"
)

private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = makeBodyLoweringPhase(
    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "WrapInlineDeclarationsWithReifiedTypeParametersLowering",
    description = "Wrap inline declarations with reified type parameters"
)

private val saveInlineFunctionsBeforeInlining = makeDeclarationTransformerPhase(
    ::SaveInlineFunctionsBeforeInlining,
    name = "SaveInlineFunctionsBeforeInlining",
    description = "Save inline function before inlining",
    prerequisite = setOf(
        expectDeclarationsRemovingPhase, sharedVariablesLoweringPhase,
        localClassesInInlineLambdasPhase, localClassesExtractionFromInlineFunctionsPhase,
        syntheticAccessorLoweringPhase, wrapInlineDeclarationsWithReifiedTypeParametersLowering
    )
)

private val functionInliningPhase = makeBodyLoweringPhase(
    {
        FunctionInlining(
            it,
            JsInlineFunctionResolver(it),
            it.innerClassesSupport,
            allowExternalInlining = true,
            useTypeParameterUpperBound = true,
            alwaysCreateTemporaryVariablesForArguments = true,
            inlineArgumentsWithTheirOriginalTypeAndOffset = true
        )
    },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(saveInlineFunctionsBeforeInlining)
)

private val copyInlineFunctionBodyLoweringPhase = makeDeclarationTransformerPhase(
    ::CopyInlineFunctionBodyLowering,
    name = "CopyInlineFunctionBody",
    description = "Copy inline function body",
    prerequisite = setOf(functionInliningPhase)
)

private val removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase = makeDeclarationTransformerPhase(
    { RemoveInlineDeclarationsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    description = "Remove Inline functions with reified parameters from context",
    prerequisite = setOf(functionInliningPhase)
)

private val captureStackTraceInThrowablesPhase = makeBodyLoweringPhase(
    ::CaptureStackTraceInThrowables,
    name = "CaptureStackTraceInThrowables",
    description = "Capture stack trace in Throwable constructors"
)

private val throwableSuccessorsLoweringPhase = makeBodyLoweringPhase(
    { context ->
        context.run {
            val extendThrowableSymbol =
                if (es6mode) setPropertiesToThrowableInstanceSymbol else extendThrowableSymbol

            ThrowableLowering(this, extendThrowableSymbol)
        }
    },
    name = "ThrowableLowering",
    description = "Link kotlin.Throwable and JavaScript Error together to provide proper interop between language and platform exceptions",
    prerequisite = setOf(captureStackTraceInThrowablesPhase)
)

private val tailrecLoweringPhase = makeBodyLoweringPhase(
    ::TailrecLowering,
    name = "TailrecLowering",
    description = "Replace `tailrec` callsites with equivalent loop"
)

private val enumClassConstructorLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumClassConstructorLowering,
    name = "EnumClassConstructorLowering",
    description = "Transform Enum Class into regular Class"
)

private val enumClassConstructorBodyLoweringPhase = makeBodyLoweringPhase(
    ::EnumClassConstructorBodyTransformer,
    name = "EnumClassConstructorBodyLowering",
    description = "Transform Enum Class into regular Class"
)

private val enumEntryInstancesLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumEntryInstancesLowering,
    name = "EnumEntryInstancesLowering",
    description = "Create instance variable for each enum entry initialized with `null`",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryInstancesBodyLoweringPhase = makeBodyLoweringPhase(
    ::EnumEntryInstancesBodyLowering,
    name = "EnumEntryInstancesBodyLowering",
    description = "Insert enum entry field initialization into correxposnding class constructors",
    prerequisite = setOf(enumEntryInstancesLoweringPhase)
)

private val enumClassCreateInitializerLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumClassCreateInitializerLowering,
    name = "EnumClassCreateInitializerLowering",
    description = "Create initializer for enum entries",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryCreateGetInstancesFunsLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumEntryCreateGetInstancesFunsLowering,
    name = "EnumEntryCreateGetInstancesFunsLowering",
    description = "Create enumEntry_getInstance functions",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumSyntheticFunsLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumSyntheticFunctionsAndPropertiesLowering,
    name = "EnumSyntheticFunctionsAndPropertiesLowering",
    description = "Implement `valueOf, `values` and `entries`",
    prerequisite = setOf(
        enumClassConstructorLoweringPhase,
        enumClassCreateInitializerLoweringPhase,
        enumEntryCreateGetInstancesFunsLoweringPhase,
    )
)

private val enumUsageLoweringPhase = makeBodyLoweringPhase(
    ::EnumUsageLowering,
    name = "EnumUsageLowering",
    description = "Replace enum access with invocation of corresponding function",
    prerequisite = setOf(enumEntryCreateGetInstancesFunsLoweringPhase)
)

private val externalEnumUsageLoweringPhase = makeBodyLoweringPhase(
    ::ExternalEnumUsagesLowering,
    name = "ExternalEnumUsagesLowering",
    description = "Replace external enum entry accesses with field accesses"
)

private val enumEntryRemovalLoweringPhase = makeDeclarationTransformerPhase(
    ::EnumClassRemoveEntriesLowering,
    name = "EnumEntryRemovalLowering",
    description = "Replace enum entry with corresponding class",
    prerequisite = setOf(enumUsageLoweringPhase)
)

private val callableReferenceLowering = makeBodyLoweringPhase(
    ::CallableReferenceLowering,
    name = "CallableReferenceLowering",
    description = "Build a lambda/callable reference class",
    prerequisite = setOf(functionInliningPhase, wrapInlineDeclarationsWithReifiedTypeParametersLowering)
)

private val returnableBlockLoweringPhase = makeBodyLoweringPhase(
    ::JsReturnableBlockLowering,
    name = "JsReturnableBlockLowering",
    description = "Introduce temporary variable for result and change returnable block's type to Unit",
    prerequisite = setOf(functionInliningPhase)
)

private val rangeContainsLoweringPhase = makeBodyLoweringPhase(
    ::RangeContainsLowering,
    name = "RangeContainsLowering",
    description = "[Optimization] Optimizes calls to contains() for ClosedRanges"
)

private val forLoopsLoweringPhase = makeBodyLoweringPhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
    description = "[Optimization] For loops lowering"
)

private val enumWhenPhase = makeJsModulePhase(
    ::EnumWhenLowering,
    name = "EnumWhenLowering",
    description = "Replace `when` subjects of enum types with their ordinals"
).toModuleLowering()

private val propertyLazyInitLoweringPhase = makeBodyLoweringPhase(
    ::PropertyLazyInitLowering,
    name = "PropertyLazyInitLowering",
    description = "Make property init as lazy"
)

private val removeInitializersForLazyProperties = makeDeclarationTransformerPhase(
    ::RemoveInitializersForLazyProperties,
    name = "RemoveInitializersForLazyProperties",
    description = "Remove property initializers if they was initialized lazily"
)

private val propertyAccessorInlinerLoweringPhase = makeBodyLoweringPhase(
    ::JsPropertyAccessorInlineLowering,
    name = "PropertyAccessorInlineLowering",
    description = "[Optimization] Inline property accessors"
)

private val copyPropertyAccessorBodiesLoweringPass = makeDeclarationTransformerPhase(
    ::CopyAccessorBodyLowerings,
    name = "CopyAccessorBodyLowering",
    description = "Copy accessor bodies so that ist can be safely read in PropertyAccessorInlineLowering",
    prerequisite = setOf(propertyAccessorInlinerLoweringPhase)
)

private val booleanPropertyInExternalLowering = makeBodyLoweringPhase(
    ::BooleanPropertyInExternalLowering,
    name = "BooleanPropertyInExternalLowering",
    description = "Lowering which wrap boolean in external declarations with Boolean() call and add diagnostic for such cases"
)

private val localDelegatedPropertiesLoweringPhase = makeBodyLoweringPhase(
    { LocalDelegatedPropertiesLowering() },
    name = "LocalDelegatedPropertiesLowering",
    description = "Transform Local Delegated properties"
)

private val localDeclarationsLoweringPhase = makeBodyLoweringPhase(
    { context -> LocalDeclarationsLowering(context, suggestUniqueNames = false) },
    name = "LocalDeclarationsLowering",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localClassExtractionPhase = makeBodyLoweringPhase(
    { context -> LocalClassPopupLowering(context) },
    name = "LocalClassExtractionPhase",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(localDeclarationsLoweringPhase)
)

private val innerClassesLoweringPhase = makeDeclarationTransformerPhase(
    { context -> InnerClassesLowering(context, context.innerClassesSupport) },
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val innerClassesMemberBodyLoweringPhase = makeBodyLoweringPhase(
    { context -> InnerClassesMemberBodyLowering(context, context.innerClassesSupport) },
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val innerClassConstructorCallsLoweringPhase = makeBodyLoweringPhase(
    { context -> InnerClassConstructorCallsLowering(context, context.innerClassesSupport) },
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val suspendFunctionsLoweringPhase = makeBodyLoweringPhase(
    ::JsSuspendFunctionsLowering,
    name = "SuspendFunctionsLowering",
    description = "Transform suspend functions into CoroutineImpl instance and build state machine"
)

private val addContinuationToNonLocalSuspendFunctionsLoweringPhase = makeDeclarationTransformerPhase(
    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    name = "AddContinuationToNonLocalSuspendFunctionsLowering",
    description = "Add explicit continuation as last parameter of non-local suspend functions"
)

private val addContinuationToLocalSuspendFunctionsLoweringPhase = makeBodyLoweringPhase(
    ::AddContinuationToLocalSuspendFunctionsLowering,
    name = "AddContinuationToLocalSuspendFunctionsLowering",
    description = "Add explicit continuation as last parameter of local suspend functions"
)


private val addContinuationToFunctionCallsLoweringPhase = makeBodyLoweringPhase(
    ::AddContinuationToFunctionCallsLowering,
    name = "AddContinuationToFunctionCallsLowering",
    description = "Replace suspend function calls with calls with continuation",
    prerequisite = setOf(
        addContinuationToLocalSuspendFunctionsLoweringPhase,
        addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    )
)

private val privateMembersLoweringPhase = makeDeclarationTransformerPhase(
    ::PrivateMembersLowering,
    name = "PrivateMembersLowering",
    description = "Extract private members from classes"
)

private val privateMemberUsagesLoweringPhase = makeBodyLoweringPhase(
    ::PrivateMemberBodiesLowering,
    name = "PrivateMemberUsagesLowering",
    description = "Rewrite the private member usages"
)

private val propertyReferenceLoweringPhase = makeBodyLoweringPhase(
    ::PropertyReferenceLowering,
    name = "PropertyReferenceLowering",
    description = "Transform property references",
)

private val interopCallableReferenceLoweringPhase = makeBodyLoweringPhase(
    ::InteropCallableReferenceLowering,
    name = "InteropCallableReferenceLowering",
    description = "Interop layer for function references and lambdas",
    prerequisite = setOf(
        suspendFunctionsLoweringPhase,
        localDeclarationsLoweringPhase,
        localDelegatedPropertiesLoweringPhase,
        callableReferenceLowering
    )
)

private val defaultArgumentStubGeneratorPhase = makeDeclarationTransformerPhase(
    ::JsDefaultArgumentStubGenerator,
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val defaultArgumentPatchOverridesPhase = makeDeclarationTransformerPhase(
    ::DefaultParameterPatchOverridenSymbolsLowering,
    name = "DefaultArgumentsPatchOverrides",
    description = "Patch overrides for fake override dispatch functions",
    prerequisite = setOf(defaultArgumentStubGeneratorPhase)
)

private val defaultParameterInjectorPhase = makeBodyLoweringPhase(
    ::JsDefaultParameterInjector,
    name = "DefaultParameterInjector",
    description = "Replace callsite with default parameters with corresponding stub function",
    prerequisite = setOf(interopCallableReferenceLoweringPhase, innerClassesLoweringPhase)
)

private val defaultParameterCleanerPhase = makeDeclarationTransformerPhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Clean default parameters up"
)

private val varargLoweringPhase = makeBodyLoweringPhase(
    ::VarargLowering,
    name = "VarargLowering",
    description = "Lower vararg arguments",
    prerequisite = setOf(interopCallableReferenceLoweringPhase)
)

private val propertiesLoweringPhase = makeDeclarationTransformerPhase(
    { PropertiesLowering() },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val primaryConstructorLoweringPhase = makeDeclarationTransformerPhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
    description = "Creates primary constructor if it doesn't exist",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val delegateToPrimaryConstructorLoweringPhase = makeBodyLoweringPhase(
    ::DelegateToSyntheticPrimaryConstructor,
    name = "DelegateToSyntheticPrimaryConstructor",
    description = "Delegates to synthetic primary constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val annotationConstructorLowering = makeDeclarationTransformerPhase(
    ::AnnotationConstructorLowering,
    name = "AnnotationConstructorLowering",
    description = "Generate annotation constructor body"
)

private val initializersLoweringPhase = makeBodyLoweringPhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    description = "Merge init block and field initializers into [primary] constructor",
    prerequisite = setOf(
        enumClassConstructorLoweringPhase, primaryConstructorLoweringPhase, annotationConstructorLowering, localClassExtractionPhase
    )
)

private val initializersCleanupLoweringPhase = makeDeclarationTransformerPhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    description = "Remove non-static anonymous initializers and field init expressions",
    prerequisite = setOf(initializersLoweringPhase)
)

private val multipleCatchesLoweringPhase = makeBodyLoweringPhase(
    ::MultipleCatchesLowering,
    name = "MultipleCatchesLowering",
    description = "Replace multiple catches with single one"
)

private val errorExpressionLoweringPhase = makeBodyLoweringPhase(
    ::JsErrorExpressionLowering,
    name = "errorExpressionLoweringPhase",
    description = "Transform error expressions into simple ir code",
    prerequisite = setOf(multipleCatchesLoweringPhase)
)

private val errorDeclarationLoweringPhase = makeDeclarationTransformerPhase(
    ::JsErrorDeclarationLowering,
    name = "errorDeclarationLoweringPhase",
    description = "Transform error declarations into simple ir code"
)

private val bridgesConstructionPhase = makeDeclarationTransformerPhase(
    ::JsBridgesConstruction,
    name = "BridgesConstruction",
    description = "Generate bridges",
    prerequisite = setOf(suspendFunctionsLoweringPhase)
)

private val singleAbstractMethodPhase = makeBodyLoweringPhase(
    ::JsSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

private val typeOperatorLoweringPhase = makeBodyLoweringPhase(
    ::TypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperator with corresponding logic",
    prerequisite = setOf(
        bridgesConstructionPhase,
        removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,
        singleAbstractMethodPhase, errorExpressionLoweringPhase,
        interopCallableReferenceLoweringPhase,
    )
)

private val secondaryConstructorLoweringPhase = makeDeclarationTransformerPhase(
    ::SecondaryConstructorLowering,
    name = "SecondaryConstructorLoweringPhase",
    description = "Generate static functions for each secondary constructor",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val secondaryFactoryInjectorLoweringPhase = makeBodyLoweringPhase(
    ::SecondaryFactoryInjectorLowering,
    name = "SecondaryFactoryInjectorLoweringPhase",
    description = "Replace usage of secondary constructor with corresponding static function",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val constLoweringPhase = makeBodyLoweringPhase(
    ::ConstLowering,
    name = "ConstLowering",
    description = "Wrap Long and Char constants into constructor invocation"
)
private val inlineClassDeclarationLoweringPhase = makeDeclarationTransformerPhase(
    { InlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
    description = "Handle inline class declarations"
)

private val inlineClassUsageLoweringPhase = makeBodyLoweringPhase(
    { InlineClassLowering(it).inlineClassUsageLowering },
    name = "InlineClassUsageLowering",
    description = "Handle inline class usages",
    prerequisite = setOf(
        // Const lowering generates inline class constructors for unsigned integers
        // which should be lowered by this lowering
        constLoweringPhase
    )
)

private val autoboxingTransformerPhase = makeBodyLoweringPhase(
    { AutoboxingTransformer(it, shouldCalculateActualTypeForInlinedFunction = true) },
    name = "AutoboxingTransformer",
    description = "Insert box/unbox intrinsics"
)

private val blockDecomposerLoweringPhase = makeBodyLoweringPhase(
    ::JsBlockDecomposerLowering,
    name = "BlockDecomposerLowering",
    description = "Transform statement-like-expression nodes into pure-statement to make it easily transform into JS",
    prerequisite = setOf(typeOperatorLoweringPhase, suspendFunctionsLoweringPhase)
)

private val jsClassUsageInReflectionPhase = makeBodyLoweringPhase(
    ::JsClassUsageInReflectionLowering,
    name = "JsClassUsageInReflectionLowering",
    description = "[Optimization] Eliminate ClassReference and GetClassExpression usages in a simple case of usage raw js constructor",
    prerequisite = setOf(functionInliningPhase)
)

private val classReferenceLoweringPhase = makeBodyLoweringPhase(
    ::ClassReferenceLowering,
    name = "ClassReferenceLowering",
    description = "Handle class references",
    prerequisite = setOf(jsClassUsageInReflectionPhase)
)

private val primitiveCompanionLoweringPhase = makeBodyLoweringPhase(
    ::PrimitiveCompanionLowering,
    name = "PrimitiveCompanionLowering",
    description = "Replace common companion object access with platform one"
)

private val callsLoweringPhase = makeBodyLoweringPhase(
    ::CallsLowering,
    name = "CallsLowering",
    description = "Handle intrinsics"
)

private val staticMembersLoweringPhase = makeDeclarationTransformerPhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
    description = "Move static member declarations to top-level"
)

private val objectDeclarationLoweringPhase = makeDeclarationTransformerPhase(
    ::ObjectDeclarationLowering,
    name = "ObjectDeclarationLowering",
    description = "Create lazy object instance generator functions",
    prerequisite = setOf(enumClassCreateInitializerLoweringPhase)
)

private val invokeStaticInitializersPhase = makeBodyLoweringPhase(
    ::InvokeStaticInitializersLowering,
    name = "IntroduceStaticInitializersLowering",
    description = "Invoke companion object's initializers from companion object in object constructor",
    prerequisite = setOf(objectDeclarationLoweringPhase)
)

private val es6AddBoxParameterToConstructorsLowering = makeDeclarationTransformerPhase(
    ::ES6AddBoxParameterToConstructorsLowering,
    name = "ES6AddBoxParameterToConstructorsLowering",
    description = "Add box parameter to a constructor if needed",
)

private val es6ConstructorLowering = makeDeclarationTransformerPhase(
    ::ES6ConstructorLowering,
    name = "ES6ConstructorLowering",
    description = "Lower constructors declarations to support ES classes",
    prerequisite = setOf(es6AddBoxParameterToConstructorsLowering)
)

private val es6ConstructorUsageLowering = makeBodyLoweringPhase(
    ::ES6ConstructorCallLowering,
    name = "ES6ConstructorCallLowering",
    description = "Lower constructor usages to support ES classes",
    prerequisite = setOf(es6ConstructorLowering)
)

private val objectUsageLoweringPhase = makeBodyLoweringPhase(
    ::ObjectUsageLowering,
    name = "ObjectUsageLowering",
    description = "Transform IrGetObjectValue into instance generator call",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val escapedIdentifiersLowering = makeBodyLoweringPhase(
    ::EscapedIdentifiersLowering,
    name = "EscapedIdentifiersLowering",
    description = "Convert global variables with invalid names access to globalThis member expression"
)

private val implicitlyExportedDeclarationsMarkingLowering = makeDeclarationTransformerPhase(
    ::ImplicitlyExportedDeclarationsMarkingLowering,
    name = "ImplicitlyExportedDeclarationsMarkingLowering",
    description = "Add @JsImplicitExport annotation to declarations which are not exported but are used inside other exported declarations as a type"
)

private val cleanupLoweringPhase = makeBodyLoweringPhase(
    { CleanupLowering() },
    name = "CleanupLowering",
    description = "Clean up IR before codegen"
)

private val jsSuspendArityStorePhase = makeDeclarationTransformerPhase(
    ::JsSuspendArityStoreLowering,
    name = "JsSuspendArityStoreLowering",
    description = "Store arity for suspend functions to not remove it during DCE"
)

val constEvaluationPhase = makeJsModulePhase(
    { context ->
        // We can't inline `const val`s because this lowering can mess up incremental compilation.
        // For example, if we inline some constant located in `lib` module then we are not going to track and update its value on change.
        // The only usages of `const val`s that we allow to inline are the ones that are located at the same file as declaration.
        val configuration = IrInterpreterConfiguration(
            printOnlyExceptionMessage = true,
            platform = JsPlatforms.defaultJsPlatform,
            inlineConstVal = false
        )
        ConstEvaluationLowering(context, configuration = configuration)
    },
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`",
).toModuleLowering()

val loweringList = listOf<Lowering>(
    scriptRemoveReceiverLowering,
    validateIrBeforeLowering,
    preventExportOfSyntheticDeclarationsLowering,
    inventNamesForLocalClassesPhase,
    collectClassIdentifiersLowering,
    annotationInstantiationLowering,
    expectDeclarationsRemovingPhase,
    stripTypeAliasDeclarationsPhase,
    jsCodeOutliningPhase,
    arrayConstructorReferencePhase,
    arrayConstructorPhase,
    lateinitNullableFieldsPhase,
    lateinitDeclarationLoweringPhase,
    lateinitUsageLoweringPhase,
    sharedVariablesLoweringPhase,
    localClassesInInlineLambdasPhase,
    localClassesInInlineFunctionsPhase,
    localClassesExtractionFromInlineFunctionsPhase,
    syntheticAccessorLoweringPhase,
    wrapInlineDeclarationsWithReifiedTypeParametersLowering,
    saveInlineFunctionsBeforeInlining,
    functionInliningPhase,
    constEvaluationPhase,
    copyInlineFunctionBodyLoweringPhase,
    removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,
    createScriptFunctionsPhase,
    stringConcatenationLoweringPhase,
    callableReferenceLowering,
    singleAbstractMethodPhase,
    tailrecLoweringPhase,
    enumClassConstructorLoweringPhase,
    enumClassConstructorBodyLoweringPhase,
    localDelegatedPropertiesLoweringPhase,
    localDeclarationsLoweringPhase,
    localClassExtractionPhase,
    innerClassesLoweringPhase,
    innerClassesMemberBodyLoweringPhase,
    innerClassConstructorCallsLoweringPhase,
    jsClassUsageInReflectionPhase,
    propertiesLoweringPhase,
    primaryConstructorLoweringPhase,
    delegateToPrimaryConstructorLoweringPhase,
    annotationConstructorLowering,
    initializersLoweringPhase,
    initializersCleanupLoweringPhase,
    kotlinNothingValueExceptionPhase,
    collectClassDefaultConstructorsPhase,
    // Common prefix ends
    enumWhenPhase,
    enumEntryInstancesLoweringPhase,
    enumEntryInstancesBodyLoweringPhase,
    enumClassCreateInitializerLoweringPhase,
    enumEntryCreateGetInstancesFunsLoweringPhase,
    enumSyntheticFunsLoweringPhase,
    enumUsageLoweringPhase,
    externalEnumUsageLoweringPhase,
    enumEntryRemovalLoweringPhase,
    suspendFunctionsLoweringPhase,
    propertyReferenceLoweringPhase,
    interopCallableReferenceLoweringPhase,
    jsSuspendArityStorePhase,
    addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    addContinuationToLocalSuspendFunctionsLoweringPhase,
    addContinuationToFunctionCallsLoweringPhase,
    returnableBlockLoweringPhase,
    rangeContainsLoweringPhase,
    forLoopsLoweringPhase,
    primitiveCompanionLoweringPhase,
    propertyLazyInitLoweringPhase,
    removeInitializersForLazyProperties,
    propertyAccessorInlinerLoweringPhase,
    copyPropertyAccessorBodiesLoweringPass,
    booleanPropertyInExternalLowering,
    privateMembersLoweringPhase,
    privateMemberUsagesLoweringPhase,
    defaultArgumentStubGeneratorPhase,
    defaultArgumentPatchOverridesPhase,
    defaultParameterInjectorPhase,
    defaultParameterCleanerPhase,
    captureStackTraceInThrowablesPhase,
    throwableSuccessorsLoweringPhase,
    varargLoweringPhase,
    multipleCatchesLoweringPhase,
    errorExpressionLoweringPhase,
    errorDeclarationLoweringPhase,
    bridgesConstructionPhase,
    typeOperatorLoweringPhase,
    secondaryConstructorLoweringPhase,
    secondaryFactoryInjectorLoweringPhase,
    classReferenceLoweringPhase,
    constLoweringPhase,
    inlineClassDeclarationLoweringPhase,
    inlineClassUsageLoweringPhase,
    autoboxingTransformerPhase,
    objectDeclarationLoweringPhase,
    blockDecomposerLoweringPhase,
    invokeStaticInitializersPhase,
    objectUsageLoweringPhase,
    es6AddBoxParameterToConstructorsLowering,
    es6ConstructorLowering,
    es6ConstructorUsageLowering,
    callsLoweringPhase,
    escapedIdentifiersLowering,
    implicitlyExportedDeclarationsMarkingLowering,
    cleanupLoweringPhase,
    // Currently broken due to static members lowering making single-open-class
    // files non-recognizable as single-class files
    validateIrAfterLowering,
)

val jsPhases = SameTypeNamedCompilerPhase(
    name = "IrModuleLowering",
    description = "IR module lowering",
    lower = loweringList.toCompilerPhase(),
    actions = setOf(defaultDumper.toMultiModuleAction(), validationAction.toMultiModuleAction()),
    nlevels = 1
)

private val es6CollectConstructorsWhichNeedBoxParameterLowering = makeDeclarationTransformerPhase(
    ::ES6CollectConstructorsWhichNeedBoxParameters,
    name = "ES6CollectConstructorsWhichNeedBoxParameters",
    description = "[Optimization] Collect all of the constructors which requires box parameter",
)

private val es6BoxParameterOptimization = makeBodyLoweringPhase(
    ::ES6ConstructorBoxParameterOptimizationLowering,
    name = "ES6ConstructorBoxParameterOptimizationLowering",
    description = "[Optimization] Remove box parameter from the constructors which don't require box parameter",
    prerequisite = setOf(es6CollectConstructorsWhichNeedBoxParameterLowering)
)

private val es6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering = makeDeclarationTransformerPhase(
    ::ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering,
    name = "ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering",
    description = "[Optimization] Collect all of the constructors which could be translated into a regular constructor",
)

private val es6PrimaryConstructorOptimizationLowering = makeDeclarationTransformerPhase(
    ::ES6PrimaryConstructorOptimizationLowering,
    name = "ES6PrimaryConstructorOptimizationLowering",
    description = "[Optimization] Replace synthetically generated static fabric method with a plain old ES6 constructors whenever it's possible",
    prerequisite = setOf(es6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering)
)

private val es6PrimaryConstructorUsageOptimizationLowering = makeBodyLoweringPhase(
    ::ES6PrimaryConstructorUsageOptimizationLowering,
    name = "ES6PrimaryConstructorUsageOptimizationLowering",
    description = "[Optimization] Replace usage of synthetically generated static fabric method with a plain old ES6 constructors whenever it's possible",
    prerequisite = setOf(es6BoxParameterOptimization, es6PrimaryConstructorOptimizationLowering)
)

private val purifyObjectInstanceGetters = makeDeclarationTransformerPhase(
    ::PurifyObjectInstanceGettersLowering,
    name = "PurifyObjectInstanceGettersLowering",
    description = "[Optimization] Make object instance getter functions pure whenever it's possible",
)

private val inlineObjectsWithPureInitialization = makeBodyLoweringPhase(
    ::InlineObjectsWithPureInitializationLowering,
    name = "InlineObjectsWithPureInitializationLowering",
    description = "[Optimization] Inline object instance fields getters whenever it's possible",
    prerequisite = setOf(purifyObjectInstanceGetters)
)

val optimizationLoweringList = listOf<Lowering>(
    es6CollectConstructorsWhichNeedBoxParameterLowering,
    es6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering,
    es6BoxParameterOptimization,
    es6PrimaryConstructorOptimizationLowering,
    es6PrimaryConstructorUsageOptimizationLowering,
    purifyObjectInstanceGetters,
    inlineObjectsWithPureInitialization
)

val jsOptimizationPhases = SameTypeNamedCompilerPhase(
    name = "IrModuleOptimizationLowering",
    description = "IR module optimization lowering",
    lower = optimizationLoweringList.toCompilerPhase(),
    actions = setOf(defaultDumper.toMultiModuleAction(), validationAction.toMultiModuleAction()),
    nlevels = 1
)
