/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.wasm.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

private fun makeWasmModulePhase(
    lowering: (WasmBackendContext) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<WasmBackendContext, *>> = emptySet()
): NamedCompilerPhase<WasmBackendContext, IrModuleFragment> =
    makeIrModulePhase(
        lowering, name, description, prerequisite, actions = setOf(validationAction, defaultDumper)
    )

private fun makeCustomWasmModulePhase(
    op: (WasmBackendContext, IrModuleFragment) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<NamedCompilerPhase<WasmBackendContext, *>> = emptySet()
): NamedCompilerPhase<WasmBackendContext, IrModuleFragment> =
    makeCustomPhase(
        op, name, description, prerequisite, actions = setOf(defaultDumper, validationAction), nlevels = 0,
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

private val stringConstructorLowering = makeWasmModulePhase(
    ::SimpleStringConcatenationLowering,
    name = "StringConcatenation",
    description = "String concatenation lowering"
)

private val lateinitNullableFieldsPhase = makeWasmModulePhase(
    ::NullableFieldsForLateinitCreationLowering,
    name = "LateinitNullableFields",
    description = "Create nullable fields for lateinit properties"
)

private val lateinitDeclarationLoweringPhase = makeWasmModulePhase(
    ::NullableFieldsDeclarationLowering,
    name = "LateinitDeclarations",
    description = "Reference nullable fields from properties and getters + insert checks"
)

private val lateinitUsageLoweringPhase = makeWasmModulePhase(
    ::LateinitUsageLowering,
    name = "LateinitUsage",
    description = "Insert checks for lateinit field references"
)


// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase
private val provisionalFunctionExpressionPhase = makeWasmModulePhase(
    { ProvisionalFunctionExpressionLowering() },
    name = "FunctionExpression",
    description = "Transform IrFunctionExpression to a local function reference"
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
    description = "Replace `tailrec` call sites with equivalent loop"
)

private val enumClassConstructorLoweringPhase = makeWasmModulePhase(
    ::EnumClassConstructorLowering,
    name = "EnumClassConstructorLowering",
    description = "Transform Enum Class into regular Class"
)

private val enumClassConstructorBodyLoweringPhase = makeWasmModulePhase(
    ::EnumClassConstructorBodyTransformer,
    name = "EnumClassConstructorBodyLowering",
    description = "Transform Enum Class into regular Class"
)


private val enumEntryInstancesLoweringPhase = makeWasmModulePhase(
    ::EnumEntryInstancesLowering,
    name = "EnumEntryInstancesLowering",
    description = "Create instance variable for each enum entry initialized with `null`",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryInstancesBodyLoweringPhase = makeWasmModulePhase(
    ::EnumEntryInstancesBodyLowering,
    name = "EnumEntryInstancesBodyLowering",
    description = "Insert enum entry field initialization into corresponding class constructors",
    prerequisite = setOf(enumEntryInstancesLoweringPhase)
)

private val enumClassCreateInitializerLoweringPhase = makeWasmModulePhase(
    ::EnumClassCreateInitializerLowering,
    name = "EnumClassCreateInitializerLowering",
    description = "Create initializer for enum entries",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryCreateGetInstancesFunsLoweringPhase = makeWasmModulePhase(
    ::EnumEntryCreateGetInstancesFunsLowering,
    name = "EnumEntryCreateGetInstancesFunsLowering",
    description = "Create enumEntry_getInstance functions",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumSyntheticFunsLoweringPhase = makeWasmModulePhase(
    ::EnumSyntheticFunctionsLowering,
    name = "EnumSyntheticFunctionsLowering",
    description = "Implement `valueOf` and `values`",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumUsageLoweringPhase = makeWasmModulePhase(
    ::EnumUsageLowering,
    name = "EnumUsageLowering",
    description = "Replace enum access with invocation of corresponding function",
    prerequisite = setOf(enumEntryCreateGetInstancesFunsLoweringPhase)
)

private val enumEntryRemovalLoweringPhase = makeWasmModulePhase(
    ::EnumClassRemoveEntriesLowering,
    name = "EnumEntryRemovalLowering",
    description = "Replace enum entry with corresponding class",
    prerequisite = setOf(enumUsageLoweringPhase)
)


private val sharedVariablesLoweringPhase = makeWasmModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    description = "Box captured mutable variables"
)

private val callableReferencePhase = makeWasmModulePhase(
    ::WasmCallableReferenceLowering,
    name = "WasmCallableReferenceLowering",
    description = "Handle callable references"
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
    { context -> InnerClassesLowering(context, context.innerClassesSupport) },
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val innerClassesMemberBodyLoweringPhase = makeWasmModulePhase(
    { context -> InnerClassesMemberBodyLowering(context, context.innerClassesSupport) },
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val innerClassConstructorCallsLoweringPhase = makeWasmModulePhase(
    { context -> InnerClassConstructorCallsLowering(context, context.innerClassesSupport) },
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val defaultArgumentStubGeneratorPhase = makeWasmModulePhase(
    ::DefaultArgumentStubGenerator,
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val defaultArgumentPatchOverridesPhase = makeWasmModulePhase(
    ::DefaultParameterPatchOverridenSymbolsLowering,
    name = "DefaultArgumentsPatchOverrides",
    description = "Patch overrides for fake override dispatch functions",
    prerequisite = setOf(defaultArgumentStubGeneratorPhase)
)

private val defaultParameterInjectorPhase = makeWasmModulePhase(
    { context -> DefaultParameterInjector(context, skipExternalMethods = true) },
    name = "DefaultParameterInjector",
    description = "Replace call site with default parameters with corresponding stub function",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val defaultParameterCleanerPhase = makeWasmModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Clean default parameters up"
)

private val propertiesLoweringPhase = makeWasmModulePhase(
    { PropertiesLowering() },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val primaryConstructorLoweringPhase = makeWasmModulePhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
    description = "Creates primary constructor if it doesn't exist"
)

private val delegateToPrimaryConstructorLoweringPhase = makeWasmModulePhase(
    ::DelegateToSyntheticPrimaryConstructor,
    name = "DelegateToSyntheticPrimaryConstructor",
    description = "Delegates to synthetic primary constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val initializersLoweringPhase = makeWasmModulePhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    description = "Merge init block and field initializers into [primary] constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase, localClassExtractionPhase)
)

private val initializersCleanupLoweringPhase = makeWasmModulePhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    description = "Remove non-static anonymous initializers and field init expressions",
    prerequisite = setOf(initializersLoweringPhase)
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
    ::WasmBridgesConstruction,
    name = "BridgesConstruction",
    description = "Generate bridges"
)

private val inlineClassDeclarationLoweringPhase = makeWasmModulePhase(
    { InlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
    description = "Handle inline class declarations"
)

private val inlineClassUsageLoweringPhase = makeWasmModulePhase(
    { InlineClassLowering(it).inlineClassUsageLowering },
    name = "InlineClassUsageLowering",
    description = "Handle inline class usages"
)

private val autoboxingTransformerPhase = makeWasmModulePhase(
    { context -> AutoboxingTransformer(context) },
    name = "AutoboxingTransformer",
    description = "Insert box/unbox intrinsics"
)

private val wasmNullSpecializationLowering = makeWasmModulePhase(
    { context -> WasmNullCoercingLowering(context) },
    name = "WasmNullCoercingLowering",
    description = "Specialize assigning Nothing? values to other types."
)

private val staticMembersLoweringPhase = makeWasmModulePhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
    description = "Move static member declarations to top-level"
)

private val wasmVarargExpressionLoweringPhase = makeWasmModulePhase(
    ::WasmVarargExpressionLowering,
    name = "WasmVarargExpressionLowering",
    description = "Lower varargs"
)

private val wasmThrowDebugLoweringPhase = makeWasmModulePhase(
    ::WasmThrowDebugLowering,
    name = "WasmThrowDebugLowering",
    description = "Instrument throws with debug print information"
)

private val fieldInitializersLoweringPhase = makeWasmModulePhase(
    ::FieldInitializersLowering,
    name = "FieldInitializersLowering",
    description = "Move field initializers to start function"
)

private val builtInsLoweringPhase0 = makeWasmModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering0",
    description = "Lower IR builtins 0"
)


private val builtInsLoweringPhase = makeWasmModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering",
    description = "Lower IR builtins"
)

private val objectDeclarationLoweringPhase = makeWasmModulePhase(
    ::ObjectDeclarationLowering,
    name = "ObjectDeclarationLowering",
    description = "Create lazy object instance generator functions"
)

private val objectUsageLoweringPhase = makeWasmModulePhase(
    ::ObjectUsageLowering,
    name = "ObjectUsageLowering",
    description = "Transform IrGetObjectValue into instance generator call"
)

private val typeOperatorLoweringPhase = makeWasmModulePhase(
    ::WasmTypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperator with corresponding logic"
)

private val genericReturnTypeLowering = makeWasmModulePhase(
    ::GenericReturnTypeLowering,
    name = "GenericReturnTypeLowering",
    description = "Cast calls to functions with generic return types"
)

private val eraseVirtualDispatchReceiverParametersTypes = makeWasmModulePhase(
    ::EraseVirtualDispatchReceiverParametersTypes,
    name = "EraseVirtualDispatchReceiverParametersTypes",
    description = "Erase types of virtual dispatch receivers to Any"
)

private val virtualDispatchReceiverExtractionPhase = makeWasmModulePhase(
    ::VirtualDispatchReceiverExtraction,
    name = "VirtualDispatchReceiverExtraction",
    description = "Eliminate side-effects in dispatch receivers of virtual function calls"
)

private val forLoopsLoweringPhase = makeWasmModulePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
    description = "[Optimization] For loops lowering"
)

val wasmPhases = NamedCompilerPhase(
    name = "IrModuleLowering",
    description = "IR module lowering",
    lower = validateIrBeforeLowering then
            excludeDeclarationsFromCodegenPhase then
            expectDeclarationsRemovingPhase then

            // TODO: Need some helpers from stdlib
            // arrayConstructorPhase then

            functionInliningPhase then
            provisionalFunctionExpressionPhase then
            lateinitNullableFieldsPhase then
            lateinitDeclarationLoweringPhase then
            lateinitUsageLoweringPhase then
            tailrecLoweringPhase then

            enumClassConstructorLoweringPhase then
            enumClassConstructorBodyLoweringPhase then

            sharedVariablesLoweringPhase then
            callableReferencePhase then
            localDelegatedPropertiesLoweringPhase then
            localDeclarationsLoweringPhase then
            localClassExtractionPhase then
            innerClassesLoweringPhase then
            innerClassesMemberBodyLoweringPhase then
            innerClassConstructorCallsLoweringPhase then
            propertiesLoweringPhase then
            primaryConstructorLoweringPhase then
            delegateToPrimaryConstructorLoweringPhase then
            initializersLoweringPhase then
            initializersCleanupLoweringPhase then
            // Common prefix ends

            enumEntryInstancesLoweringPhase then
            enumEntryInstancesBodyLoweringPhase then
            enumClassCreateInitializerLoweringPhase then
            enumEntryCreateGetInstancesFunsLoweringPhase then
            enumSyntheticFunsLoweringPhase then
            enumUsageLoweringPhase then
            enumEntryRemovalLoweringPhase then

//            TODO: Requires stdlib
//            suspendFunctionsLoweringPhase then

            stringConstructorLowering then
            returnableBlockLoweringPhase then

            forLoopsLoweringPhase then

            defaultArgumentStubGeneratorPhase then
            defaultArgumentPatchOverridesPhase then
            defaultParameterInjectorPhase then
            defaultParameterCleanerPhase then
            removeInlineFunctionsWithReifiedTypeParametersLoweringPhase then

//            TODO:
//            multipleCatchesLoweringPhase then
//            classReferenceLoweringPhase then

            wasmVarargExpressionLoweringPhase then
            inlineClassDeclarationLoweringPhase then
            inlineClassUsageLoweringPhase then

            eraseVirtualDispatchReceiverParametersTypes then
            bridgesConstructionPhase then
            objectDeclarationLoweringPhase then
            fieldInitializersLoweringPhase then
            genericReturnTypeLowering then

            // Replace builtins before autoboxing
            builtInsLoweringPhase0 then

            autoboxingTransformerPhase then
            objectUsageLoweringPhase then
            typeOperatorLoweringPhase then

            // Clean up built-ins after type operator lowering
            builtInsLoweringPhase then

            virtualDispatchReceiverExtractionPhase then
            wasmThrowDebugLoweringPhase then
            staticMembersLoweringPhase then
            wasmNullSpecializationLowering then
            validateIrAfterLowering
)
