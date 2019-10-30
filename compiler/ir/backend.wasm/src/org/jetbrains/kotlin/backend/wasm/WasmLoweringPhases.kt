/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.wasm.lower.BuiltInsLowering
import org.jetbrains.kotlin.backend.wasm.lower.WasmBlockDecomposerLowering
import org.jetbrains.kotlin.backend.wasm.lower.excludeDeclarationsFromCodegen
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

private fun ClassLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { runOnFilePostfix(it) }

private fun makeWasmModulePhase(
    lowering: (WasmBackendContext) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<AnyNamedPhase> = emptySet()
) = makeIrModulePhase<WasmBackendContext>(lowering, name, description, prerequisite, actions = setOf(validationAction, defaultDumper))

private fun makeCustomWasmModulePhase(
    op: (WasmBackendContext, IrModuleFragment) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrModulePhase(
    name,
    description,
    prerequisite,
    actions = setOf(defaultDumper, validationAction),
    nlevels = 0,
    lower = object : SameTypeCompilerPhase<WasmBackendContext, IrModuleFragment> {
        override fun invoke(
            phaseConfig: PhaseConfig,
            phaserState: PhaserState<IrModuleFragment>,
            context: WasmBackendContext,
            input: IrModuleFragment
        ): IrModuleFragment {
            op(context, input)
            return input
        }
    }
)

private val validateIrBeforeLowering = makeCustomWasmModulePhase(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val validateIrAfterLowering = makeCustomWasmModulePhase(
    { context, module -> validationCallback(context, module) },
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
)

private val expectDeclarationsRemovingPhase = makeWasmModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

private val lateinitLoweringPhase = makeWasmModulePhase(
    ::LateinitLowering,
    name = "LateinitLowering",
    description = "Insert checks for lateinit field references"
)

// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase
private val provisionalFunctionExpressionPhase = makeWasmModulePhase(
    { ProvisionalFunctionExpressionLowering() },
    name = "FunctionExpression",
    description = "Transform IrFunctionExpression to a local function reference"
)

private val arrayConstructorPhase = makeWasmModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    description = "Transform `Array(size) { index -> value }` into a loop"
)

private val functionInliningPhase = makeCustomWasmModulePhase(
    { context, module ->
        FunctionInlining(context).inline(module)
        module.patchDeclarationParents()
    },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(expectDeclarationsRemovingPhase)
)

private val removeInlineFunctionsWithReifiedTypeParametersLoweringPhase = makeWasmModulePhase(
    { RemoveInlineFunctionsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    description = "Remove Inline functions with reified parameters from context",
    prerequisite = setOf(functionInliningPhase)
)

private val tailrecLoweringPhase = makeWasmModulePhase(
    ::TailrecLowering,
    name = "TailrecLowering",
    description = "Replace `tailrec` callsites with equivalent loop"
)

private val enumClassConstructorLoweringPhase = makeWasmModulePhase(
    ::EnumClassConstructorLowering,
    name = "EnumClassConstructorLowering",
    description = "Transform Enum Class into regular Class"
)


private val sharedVariablesLoweringPhase = makeWasmModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    description = "Box captured mutable variables"
)

private val localDelegatedPropertiesLoweringPhase = makeWasmModulePhase(
    { LocalDelegatedPropertiesLowering() },
    name = "LocalDelegatedPropertiesLowering",
    description = "Transform Local Delegated properties"
)

private val localDeclarationsLoweringPhase = makeWasmModulePhase(
    ::LocalDeclarationsLowering,
    name = "LocalDeclarationsLowering",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localClassExtractionPhase = makeWasmModulePhase(
    ::LocalClassPopupLowering,
    name = "LocalClassExtractionPhase",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(localDeclarationsLoweringPhase)
)

private val innerClassesLoweringPhase = makeWasmModulePhase(
    ::InnerClassesLowering,
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val innerClassConstructorCallsLoweringPhase = makeWasmModulePhase(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val defaultArgumentStubGeneratorPhase = makeWasmModulePhase(
    ::DefaultArgumentStubGenerator,
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val defaultParameterInjectorPhase = makeWasmModulePhase(
    { context -> DefaultParameterInjector(context, skipExternalMethods = true) },
    name = "DefaultParameterInjector",
    description = "Replace callsite with default parameters with corresponding stub function",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val defaultParameterCleanerPhase = makeWasmModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Clean default parameters up"
)

//private val jsDefaultCallbackGeneratorPhase = makeJsModulePhase(
//    ::JsDefaultCallbackGenerator,
//    name = "JsDefaultCallbackGenerator",
//    description = "Build binding for super calls with default parameters"
//)

//private val varargLoweringPhase = makeJsModulePhase(
//    ::VarargLowering,
//    name = "VarargLowering",
//    description = "Lower vararg arguments"
//)

private val propertiesLoweringPhase = makeWasmModulePhase(
    { context -> PropertiesLowering(context, skipExternalProperties = true, generateAnnotationFields = true) },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val primaryConstructorLoweringPhase = makeWasmModulePhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
    description = "Creates primary constructor if it doesn't exist"
)

private val initializersLoweringPhase = makeWasmModulePhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    description = "Merge init block and field initializers into [primary] constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val excludeDeclarationsFromCodegenPhase = makeCustomWasmModulePhase(
    { context, module ->
        excludeDeclarationsFromCodegen(context, module)
    },
    name = "ExcludeDeclarationsFromCodegen",
    description = "Move excluded declarations to separate place"
)

private val returnableBlockLoweringPhase = makeWasmModulePhase(
    ::ReturnableBlockLowering,
    name = "ReturnableBlockLowering",
    description = "Replace returnable block with do-while loop",
    prerequisite = setOf(functionInliningPhase)
)

private val bridgesConstructionPhase = makeWasmModulePhase(
    ::BridgesConstruction,
    name = "BridgesConstruction",
    description = "Generate bridges"
)

private val inlineClassLoweringPhase = makeCustomWasmModulePhase(
    { context, module ->
        InlineClassLowering(context).run {
            inlineClassDeclarationLowering.runOnFilesPostfix(module)
            inlineClassUsageLowering.lower(module)
        }
    },
    name = "InlineClassLowering",
    description = "Handle inline classes"
)

//private val autoboxingTransformerPhase = makeJsModulePhase(
//    ::AutoboxingTransformer,
//    name = "AutoboxingTransformer",
//    description = "Insert box/unbox intrinsics"
//)

private val blockDecomposerLoweringPhase = makeCustomWasmModulePhase(
    { context, module ->
        WasmBlockDecomposerLowering(context).lower(module)
        module.patchDeclarationParents()
    },
    name = "BlockDecomposerLowering",
    description = "Transform statement-like-expression nodes into pure-statement to make it easily transform into JS"
)

//private val classReferenceLoweringPhase = makeJsModulePhase(
//    ::ClassReferenceLowering,
//    name = "ClassReferenceLowering",
//    description = "Handle class references"
//)
//
//private val primitiveCompanionLoweringPhase = makeJsModulePhase(
//    ::PrimitiveCompanionLowering,
//    name = "PrimitiveCompanionLowering",
//    description = "Replace common companion object access with platform one"
//)
//
//private val constLoweringPhase = makeJsModulePhase(
//    ::ConstLowering,
//    name = "ConstLowering",
//    description = "Wrap Long and Char constants into constructor invocation"
//)
//
//private val callsLoweringPhase = makeJsModulePhase(
//    ::CallsLowering,
//    name = "CallsLowering",
//    description = "Handle intrinsics"
//)
//
//private val testGenerationPhase = makeJsModulePhase(
//    ::TestGenerator,
//    name = "TestGenerationLowering",
//    description = "Generate invocations to kotlin.test suite and test functions"
//)
//
private val staticMembersLoweringPhase = makeWasmModulePhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
    description = "Move static member declarations to top-level"
)

private val builtInsLoweringPhase = makeWasmModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering",
    description = "Lower IR buildins"
)

private val objectDeclarationLoweringPhase = makeCustomWasmModulePhase(
    { context, module -> ObjectUsageLowering(context, context.objectToGetInstanceFunction).lower(module) },
    name = "ObjectDeclarationLowering",
    description = "Create lazy object instance generator functions"
)

private val objectUsageLoweringPhase = makeCustomWasmModulePhase(
    { context, module -> ObjectUsageLowering(context, context.objectToGetInstanceFunction).lower(module) },
    name = "ObjectUsageLowering",
    description = "Transform IrGetObjectValue into instance generator call"
)

val wasmPhases = namedIrModulePhase<WasmBackendContext>(
    name = "IrModuleLowering",
    description = "IR module lowering",
    lower = validateIrBeforeLowering then
            excludeDeclarationsFromCodegenPhase then
            expectDeclarationsRemovingPhase then
            provisionalFunctionExpressionPhase then

            // TODO: Need some helpers from stdlib
            // arrayConstructorPhase then

            functionInliningPhase then
            lateinitLoweringPhase then
            tailrecLoweringPhase then

            enumClassConstructorLoweringPhase then

            sharedVariablesLoweringPhase then
            localDelegatedPropertiesLoweringPhase then
            localDeclarationsLoweringPhase then
            localClassExtractionPhase then
            innerClassesLoweringPhase then
            innerClassConstructorCallsLoweringPhase then
            propertiesLoweringPhase then
            primaryConstructorLoweringPhase then
            initializersLoweringPhase then
            // Common prefix ends

            builtInsLoweringPhase then

//            TODO: Commonize enumEntryToGetInstanceFunction
//                  Commonize array literal creation
//                  Extract external enum lowering to JS part
//
//            enumClassLoweringPhase then
//            enumUsageLoweringPhase then


//            TODO: Requires stdlib
//            suspendFunctionsLoweringPhase then

            returnableBlockLoweringPhase then

//            TODO: Callable reference lowering is too JS specific.
//                  Should we reuse JVM or Native lowering?
//            callableReferenceLoweringPhase then

            defaultArgumentStubGeneratorPhase then
            defaultParameterInjectorPhase then
            defaultParameterCleanerPhase then

//          TODO: Investigate
//            jsDefaultCallbackGeneratorPhase then

            removeInlineFunctionsWithReifiedTypeParametersLoweringPhase then


//            TODO: Varargs are too platform-specific. Reimplement.
//            varargLoweringPhase then

//            TODO: Investigate exception proposal
//            multipleCatchesLoweringPhase then

            bridgesConstructionPhase then

//            TODO: Reimplement
//            typeOperatorLoweringPhase then

//            TODO: Reimplement
//            secondaryConstructorLoweringPhase then
//            secondaryFactoryInjectorLoweringPhase then

//            TODO: Reimplement
//            classReferenceLoweringPhase then

            inlineClassLoweringPhase then

//            TODO: Commonize box/unbox intrinsics
//            autoboxingTransformerPhase then

            blockDecomposerLoweringPhase then

//            TODO: Reimplement
//            constLoweringPhase then

            objectDeclarationLoweringPhase then
            objectUsageLoweringPhase then
            staticMembersLoweringPhase then

            validateIrAfterLowering
)