/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.wasm.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.JsSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

private fun List<CompilerPhase<WasmBackendContext, IrModuleFragment, IrModuleFragment>>.toCompilerPhase() =
    reduce { acc, lowering -> acc.then(lowering) }

private val validateIrBeforeLowering = makeIrModulePhase(
    ::IrValidationBeforeLoweringPhase,
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val validateIrAfterInliningPhase = makeIrModulePhase(
    ::IrValidationAfterInliningAllFunctionsPhase,
    name = "IrValidationAfterInliningAllFunctionsPhase",
    description = "Validate IR after all functions have been inlined",
)

private val validateIrAfterLowering = makeIrModulePhase(
    ::IrValidationAfterLoweringPhase,
    name = "ValidateIrAfterLowering",
    description = "Validate IR after lowering"
)

private val generateTests = makeIrModulePhase(
    ::GenerateWasmTests,
    name = "GenerateTests",
    description = "Generates code to execute kotlin.test cases"
)

private val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

private val stringConcatenationLowering = makeIrModulePhase(
    ::StringConcatenationLowering,
    name = "StringConcatenation",
    description = "String concatenation lowering"
)

private val lateinitNullableFieldsPhase = makeIrModulePhase(
    ::NullableFieldsForLateinitCreationLowering,
    name = "LateinitNullableFields",
    description = "Create nullable fields for lateinit properties"
)

private val lateinitDeclarationLoweringPhase = makeIrModulePhase(
    ::NullableFieldsDeclarationLowering,
    name = "LateinitDeclarations",
    description = "Reference nullable fields from properties and getters + insert checks"
)

private val lateinitUsageLoweringPhase = makeIrModulePhase(
    ::LateinitUsageLowering,
    name = "LateinitUsage",
    description = "Insert checks for lateinit field references"
)

private val rangeContainsLoweringPhase = makeIrModulePhase(
    ::RangeContainsLowering,
    name = "RangeContainsLowering",
    description = "[Optimization] Optimizes calls to contains() for ClosedRanges"
)

private val inlineCallableReferenceToLambdaPhase = makeIrModulePhase(
    ::WasmInlineCallableReferenceToLambdaPhase,
    name = "WasmInlineCallableReferenceToLambdaPhase",
    description = "Transform all callable reference (including defaults) to inline lambdas, mark inline lambdas for later passes"
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    description = "Transform `Array(size) { index -> value }` into a loop",
    prerequisite = setOf(inlineCallableReferenceToLambdaPhase)
)

private val sharedVariablesLoweringPhase = makeIrModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    description = "Box captured mutable variables",
    prerequisite = setOf(
        lateinitDeclarationLoweringPhase,
        lateinitUsageLoweringPhase
    )
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
    description = "Extract local classes from inline lambdas",
)

private val localClassesInInlineFunctionsPhase = makeIrModulePhase(
    ::LocalClassesInInlineFunctionsLowering,
    name = "LocalClassesInInlineFunctionsPhase",
    description = "Extract local classes from inline functions",
)

private val localClassesExtractionFromInlineFunctionsPhase = makeIrModulePhase(
    { context -> LocalClassesExtractionFromInlineFunctionsLowering(context) },
    name = "localClassesExtractionFromInlineFunctionsPhase",
    description = "Move local classes from inline functions into nearest declaration container",
    prerequisite = setOf(localClassesInInlineFunctionsPhase)
)


private val wrapInlineDeclarationsWithReifiedTypeParametersPhase = makeIrModulePhase(
    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "WrapInlineDeclarationsWithReifiedTypeParametersPhase",
    description = "Wrap inline declarations with reified type parameters"
)

private val functionInliningPhase = makeIrModulePhase(
    ::WasmFunctionInlining,
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(
        expectDeclarationsRemovingPhase,
        wrapInlineDeclarationsWithReifiedTypeParametersPhase,
        localClassesInInlineLambdasPhase,
    )
)

private val removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase = makeIrModulePhase(
    { RemoveInlineDeclarationsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    description = "Remove Inline functions with reified parameters from context",
    prerequisite = setOf(functionInliningPhase)
)

private val tailrecLoweringPhase = makeIrModulePhase(
    ::TailrecLowering,
    name = "TailrecLowering",
    description = "Replace `tailrec` call sites with equivalent loop"
)

private val wasmStringSwitchOptimizerLowering = makeIrModulePhase(
    ::WasmStringSwitchOptimizerLowering,
    name = "WasmStringSwitchOptimizerLowering",
    description = "Replace when with constant string cases to binary search by string hashcodes"
)

private val jsCodeCallsLowering = makeIrModulePhase(
    ::JsCodeCallsLowering,
    name = "JsCodeCallsLowering",
    description = "Lower calls to js('code') into @JsFun",
)

private val complexExternalDeclarationsToTopLevelFunctionsLowering = makeIrModulePhase(
    ::ComplexExternalDeclarationsToTopLevelFunctionsLowering,
    name = "ComplexExternalDeclarationsToTopLevelFunctionsLowering",
    description = "Lower complex external declarations to top-level functions",
)

private val complexExternalDeclarationsUsagesLowering = makeIrModulePhase(
    ::ComplexExternalDeclarationsUsageLowering,
    name = "ComplexExternalDeclarationsUsageLowering",
    description = "Lower usages of complex external declarations",
)

private val jsInteropFunctionsLowering = makeIrModulePhase(
    ::JsInteropFunctionsLowering,
    name = "JsInteropFunctionsLowering",
    description = "Create delegates for JS interop",
)

private val jsInteropFunctionCallsLowering = makeIrModulePhase(
    ::JsInteropFunctionCallsLowering,
    name = "JsInteropFunctionCallsLowering",
    description = "Replace calls to delegates",
)

private val enumWhenPhase = makeIrModulePhase(
    ::EnumWhenLowering,
    name = "EnumWhenLowering",
    description = "[Optimization] Replace `when` subjects of enum types with their ordinals"
)

private val enumClassConstructorLoweringPhase = makeIrModulePhase(
    ::EnumClassConstructorLowering,
    name = "EnumClassConstructorLowering",
    description = "Transform Enum Class into regular Class"
)

private val enumClassConstructorBodyLoweringPhase = makeIrModulePhase(
    ::EnumClassConstructorBodyTransformer,
    name = "EnumClassConstructorBodyLowering",
    description = "Transform Enum Class into regular Class"
)

private val enumEntryInstancesLoweringPhase = makeIrModulePhase(
    ::EnumEntryInstancesLowering,
    name = "EnumEntryInstancesLowering",
    description = "Create instance variable for each enum entry initialized with `null`",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryInstancesBodyLoweringPhase = makeIrModulePhase(
    ::EnumEntryInstancesBodyLowering,
    name = "EnumEntryInstancesBodyLowering",
    description = "Insert enum entry field initialization into corresponding class constructors",
    prerequisite = setOf(enumEntryInstancesLoweringPhase)
)

private val enumClassCreateInitializerLoweringPhase = makeIrModulePhase(
    ::EnumClassCreateInitializerLowering,
    name = "EnumClassCreateInitializerLowering",
    description = "Create initializer for enum entries",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryCreateGetInstancesFunsLoweringPhase = makeIrModulePhase(
    ::EnumEntryCreateGetInstancesFunsLowering,
    name = "EnumEntryCreateGetInstancesFunsLowering",
    description = "Create enumEntry_getInstance functions",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumSyntheticFunsLoweringPhase = makeIrModulePhase(
    ::EnumSyntheticFunctionsAndPropertiesLowering,
    name = "EnumSyntheticFunctionsAndPropertiesLowering",
    description = "Implement `valueOf`, `values` and `entries`",
    prerequisite = setOf(
        enumClassConstructorLoweringPhase,
        enumClassCreateInitializerLoweringPhase,
        enumEntryCreateGetInstancesFunsLoweringPhase
    )
)

private val enumUsageLoweringPhase = makeIrModulePhase(
    ::EnumUsageLowering,
    name = "EnumUsageLowering",
    description = "Replace enum access with invocation of corresponding function",
    prerequisite = setOf(enumEntryCreateGetInstancesFunsLoweringPhase)
)

private val enumEntryRemovalLoweringPhase = makeIrModulePhase(
    ::EnumClassRemoveEntriesLowering,
    name = "EnumEntryRemovalLowering",
    description = "Replace enum entry with corresponding class",
    prerequisite = setOf(enumUsageLoweringPhase)
)


private val propertyReferenceLowering = makeIrModulePhase(
    ::WasmPropertyReferenceLowering,
    name = "WasmPropertyReferenceLowering",
    description = "Lower property references"
)

private val callableReferencePhase = makeIrModulePhase(
    ::CallableReferenceLowering,
    name = "WasmCallableReferenceLowering",
    description = "Handle callable references"
)

private val singleAbstractMethodPhase = makeIrModulePhase(
    ::JsSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

private val localDelegatedPropertiesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { LocalDelegatedPropertiesLowering() },
    name = "LocalDelegatedPropertiesLowering",
    description = "Transform Local Delegated properties"
)

private val localDeclarationsLoweringPhase = makeIrModulePhase(
    ::LocalDeclarationsLowering,
    name = "LocalDeclarationsLowering",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localClassExtractionPhase = makeIrModulePhase(
    ::LocalClassPopupLowering,
    name = "LocalClassExtractionPhase",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(localDeclarationsLoweringPhase)
)

private val staticCallableReferenceLoweringPhase = makeIrModulePhase(
    ::WasmStaticCallableReferenceLowering,
    name = "WasmStaticCallableReferenceLowering",
    description = "Turn static callable references into singletons",
    prerequisite = setOf(callableReferencePhase, localClassExtractionPhase)
)

private val innerClassesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    ::InnerClassesLowering,
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val innerClassesMemberBodyLoweringPhase = makeIrModulePhase(
    ::InnerClassesMemberBodyLowering,
    name = "InnerClassesMemberBody",
    description = "Replace `this` with 'outer this' field references",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val innerClassConstructorCallsLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val suspendFunctionsLoweringPhase = makeIrModulePhase(
    ::JsSuspendFunctionsLowering,
    name = "SuspendFunctionsLowering",
    description = "Transform suspend functions into CoroutineImpl instance and build state machine"
)

private val addContinuationToNonLocalSuspendFunctionsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    name = "AddContinuationToNonLocalSuspendFunctionsLowering",
    description = "Add explicit continuation as last parameter of suspend functions"
)

private val addContinuationToFunctionCallsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToFunctionCallsLowering,
    name = "AddContinuationToFunctionCallsLowering",
    description = "Replace suspend function calls with calls with continuation",
    prerequisite = setOf(
        addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    )
)

private val generateMainFunctionWrappersPhase = makeIrModulePhase(
    ::GenerateMainFunctionWrappers,
    name = "GenerateMainFunctionWrappers",
    description = "Find main functions and generate main function wrappers",
)

private val defaultArgumentStubGeneratorPhase = makeIrModulePhase<WasmBackendContext>(
    { context -> DefaultArgumentStubGenerator(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) },
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val defaultArgumentPatchOverridesPhase = makeIrModulePhase(
    ::DefaultParameterPatchOverridenSymbolsLowering,
    name = "DefaultArgumentsPatchOverrides",
    description = "Patch overrides for fake override dispatch functions",
    prerequisite = setOf(defaultArgumentStubGeneratorPhase)
)

private val defaultParameterInjectorPhase = makeIrModulePhase(
    { context -> DefaultParameterInjector(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) },
    name = "DefaultParameterInjector",
    description = "Replace call site with default parameters with corresponding stub function",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val defaultParameterCleanerPhase = makeIrModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Clean default parameters up"
)

private val propertiesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { PropertiesLowering() },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val primaryConstructorLoweringPhase = makeIrModulePhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
    description = "Creates primary constructor if it doesn't exist"
)

private val delegateToPrimaryConstructorLoweringPhase = makeIrModulePhase(
    ::DelegateToSyntheticPrimaryConstructor,
    name = "DelegateToSyntheticPrimaryConstructor",
    description = "Delegates to synthetic primary constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val initializersLoweringPhase = makeIrModulePhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    description = "Merge init block and field initializers into [primary] constructor",
    prerequisite = setOf(primaryConstructorLoweringPhase, localClassExtractionPhase)
)

private val initializersCleanupLoweringPhase = makeIrModulePhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    description = "Remove non-static anonymous initializers and field init expressions",
    prerequisite = setOf(initializersLoweringPhase)
)

private val excludeDeclarationsFromCodegenPhase = makeIrModulePhase(
    ::ExcludeDeclarationsFromCodegen,
    name = "ExcludeDeclarationsFromCodegen",
    description = "Move excluded declarations to separate place"
)

private val unhandledExceptionLowering = makeIrModulePhase(
    ::UnhandledExceptionLowering,
    name = "UnhandledExceptionLowering",
    description = "Wrap JsExport functions with try-catch to convert unhandled Wasm exception into Js exception",
)

private val tryCatchCanonicalization = makeIrModulePhase(
    ::TryCatchCanonicalization,
    name = "TryCatchCanonicalization",
    description = "Transforms try/catch statements into canonical form supported by the wasm codegen",
    prerequisite = setOf(functionInliningPhase, unhandledExceptionLowering)
)

private val bridgesConstructionPhase = makeIrModulePhase(
    ::WasmBridgesConstruction,
    name = "BridgesConstruction",
    description = "Generate bridges"
)

private val inlineClassDeclarationLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { InlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
    description = "Handle inline class declarations"
)

private val inlineClassUsageLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { InlineClassLowering(it).inlineClassUsageLowering },
    name = "InlineClassUsageLowering",
    description = "Handle inline class usages"
)

private val autoboxingTransformerPhase = makeIrModulePhase<WasmBackendContext>(
    { context -> AutoboxingTransformer(context) },
    name = "AutoboxingTransformer",
    description = "Insert box/unbox intrinsics"
)

private val staticMembersLoweringPhase = makeIrModulePhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
    description = "Move static member declarations to top-level"
)

private val classReferenceLoweringPhase = makeIrModulePhase(
    ::WasmClassReferenceLowering,
    name = "WasmClassReferenceLowering",
    description = "Handle class references"
)

private val wasmVarargExpressionLoweringPhase = makeIrModulePhase(
    ::WasmVarargExpressionLowering,
    name = "WasmVarargExpressionLowering",
    description = "Lower varargs"
)

private val builtInsLoweringPhase0 = makeIrModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering0",
    description = "Lower IR builtins 0"
)


private val builtInsLoweringPhase = makeIrModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering",
    description = "Lower IR builtins"
)

private val associatedObjectsLowering = makeIrModulePhase(
    ::AssociatedObjectsLowering,
    name = "AssociatedObjectsLowering",
    description = "Load associated object init body",
    prerequisite = setOf(localClassExtractionPhase)
)

private val objectDeclarationLoweringPhase = makeIrModulePhase(
    ::ObjectDeclarationLowering,
    name = "ObjectDeclarationLowering",
    description = "Create lazy object instance generator functions",
    prerequisite = setOf(enumClassCreateInitializerLoweringPhase, staticCallableReferenceLoweringPhase)
)

private val invokeStaticInitializersPhase = makeIrModulePhase(
    ::InvokeStaticInitializersLowering,
    name = "InvokeStaticInitializersLowering",
    description = "Invoke companion object's initializers from companion object in object constructor",
    prerequisite = setOf(objectDeclarationLoweringPhase)
)

private val objectUsageLoweringPhase = makeIrModulePhase(
    ::ObjectUsageLowering,
    name = "ObjectUsageLowering",
    description = "Transform IrGetObjectValue into instance generator call"
)

private val explicitlyCastExternalTypesPhase = makeIrModulePhase(
    ::ExplicitlyCastExternalTypesLowering,
    name = "ExplicitlyCastExternalTypesLowering",
    description = "Add explicit casts when converting between external and non-external types"
)

private val typeOperatorLoweringPhase = makeIrModulePhase(
    ::WasmTypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperator with corresponding logic"
)

private val genericReturnTypeLowering = makeIrModulePhase(
    ::GenericReturnTypeLowering,
    name = "GenericReturnTypeLowering",
    description = "Cast calls to functions with generic return types"
)

private val eraseVirtualDispatchReceiverParametersTypes = makeIrModulePhase(
    ::EraseVirtualDispatchReceiverParametersTypes,
    name = "EraseVirtualDispatchReceiverParametersTypes",
    description = "Erase types of virtual dispatch receivers to Any"
)

private val virtualDispatchReceiverExtractionPhase = makeIrModulePhase(
    ::VirtualDispatchReceiverExtraction,
    name = "VirtualDispatchReceiverExtraction",
    description = "Eliminate side-effects in dispatch receivers of virtual function calls"
)

private val forLoopsLoweringPhase = makeIrModulePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
    description = "[Optimization] For loops lowering"
)

private val propertyLazyInitLoweringPhase = makeIrModulePhase(
    ::PropertyLazyInitLowering,
    name = "PropertyLazyInitLowering",
    description = "Make property init as lazy"
)

private val removeInitializersForLazyProperties = makeIrModulePhase(
    ::RemoveInitializersForLazyProperties,
    name = "RemoveInitializersForLazyProperties",
    description = "Remove property initializers if they was initialized lazily"
)

private val propertyAccessorInlinerLoweringPhase = makeIrModulePhase(
    ::PropertyAccessorInlineLowering,
    name = "PropertyAccessorInlineLowering",
    description = "[Optimization] Inline property accessors"
)

private val invokeOnExportedFunctionExitLowering = makeIrModulePhase(
    ::InvokeOnExportedFunctionExitLowering,
    name = "InvokeOnExportedFunctionExitLowering",
    description = "This pass is needed to call exported function exit callback for WASI",
)

private val expressionBodyTransformer = makeIrModulePhase(
    ::ExpressionBodyTransformer,
    name = "ExpressionBodyTransformer",
    description = "Replace IrExpressionBody with IrBlockBody"
)

private val unitToVoidLowering = makeIrModulePhase(
    ::UnitToVoidLowering,
    name = "UnitToVoidLowering",
    description = "Replace some Unit's with Void's"
)

private val purifyObjectInstanceGettersLoweringPhase = makeIrModulePhase(
    ::PurifyObjectInstanceGettersLowering,
    name = "PurifyObjectInstanceGettersLowering",
    description = "[Optimization] Make object instance getter functions pure whenever it's possible",
    prerequisite = setOf(objectDeclarationLoweringPhase, objectUsageLoweringPhase)
)

private val inlineObjectsWithPureInitializationLoweringPhase = makeIrModulePhase(
    ::InlineObjectsWithPureInitializationLowering,
    name = "InlineObjectsWithPureInitializationLowering",
    description = "[Optimization] Inline object instance fields getters whenever it's possible",
    prerequisite = setOf(purifyObjectInstanceGettersLoweringPhase)
)

private val whenBranchOptimiserLoweringPhase = makeIrModulePhase(
    ::WhenBranchOptimiserLowering,
    name = "WhenBranchOptimiserLowering",
    description = "[Optimization] Remove unreachable code in when's, it needed because dead paths could have an invalid IR after the inliner",
)

val constEvaluationPhase = makeIrModulePhase(
    { context ->
        val configuration = IrInterpreterConfiguration(
            printOnlyExceptionMessage = true,
            platform = WasmPlatforms.unspecifiedWasmPlatform,
        )
        ConstEvaluationLowering(context, configuration = configuration)
    },
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`",
    prerequisite = setOf(functionInliningPhase)
)

val loweringList = listOf(
    validateIrBeforeLowering,
    jsCodeCallsLowering,
    generateTests,
    excludeDeclarationsFromCodegenPhase,
    expectDeclarationsRemovingPhase,

    lateinitNullableFieldsPhase,
    lateinitDeclarationLoweringPhase,
    lateinitUsageLoweringPhase,
    rangeContainsLoweringPhase,

    sharedVariablesLoweringPhase,
    localClassesInInlineLambdasPhase,
    localClassesInInlineFunctionsPhase,
    localClassesExtractionFromInlineFunctionsPhase,

    inlineCallableReferenceToLambdaPhase,
    arrayConstructorPhase,
    wrapInlineDeclarationsWithReifiedTypeParametersPhase,

    functionInliningPhase,
    validateIrAfterInliningPhase,
    constEvaluationPhase,
    removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,

    tailrecLoweringPhase,

    enumWhenPhase,
    enumClassConstructorLoweringPhase,
    enumClassConstructorBodyLoweringPhase,
    enumEntryInstancesLoweringPhase,
    enumEntryInstancesBodyLoweringPhase,
    enumClassCreateInitializerLoweringPhase,
    enumEntryCreateGetInstancesFunsLoweringPhase,
    enumSyntheticFunsLoweringPhase,

    propertyReferenceLowering,
    callableReferencePhase,
    singleAbstractMethodPhase,
    localDelegatedPropertiesLoweringPhase,
    localDeclarationsLoweringPhase,
    localClassExtractionPhase,
    staticCallableReferenceLoweringPhase,
    innerClassesLoweringPhase,
    innerClassesMemberBodyLoweringPhase,
    innerClassConstructorCallsLoweringPhase,
    propertiesLoweringPhase,
    primaryConstructorLoweringPhase,
    delegateToPrimaryConstructorLoweringPhase,
    // Common prefix ends

    wasmStringSwitchOptimizerLowering,

    associatedObjectsLowering,

    complexExternalDeclarationsToTopLevelFunctionsLowering,
    complexExternalDeclarationsUsagesLowering,

    jsInteropFunctionsLowering,
    jsInteropFunctionCallsLowering,

    enumUsageLoweringPhase,
    enumEntryRemovalLoweringPhase,

    suspendFunctionsLoweringPhase,
    initializersLoweringPhase,
    initializersCleanupLoweringPhase,

    addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    addContinuationToFunctionCallsLoweringPhase,
    generateMainFunctionWrappersPhase,

    invokeOnExportedFunctionExitLowering,

    unhandledExceptionLowering,
    tryCatchCanonicalization,

    forLoopsLoweringPhase,
    propertyLazyInitLoweringPhase,
    removeInitializersForLazyProperties,
    propertyAccessorInlinerLoweringPhase,

    stringConcatenationLowering,

    defaultArgumentStubGeneratorPhase,
    defaultArgumentPatchOverridesPhase,
    defaultParameterInjectorPhase,
    defaultParameterCleanerPhase,

//            TODO:
//            multipleCatchesLoweringPhase,
    classReferenceLoweringPhase,

    wasmVarargExpressionLoweringPhase,
    inlineClassDeclarationLoweringPhase,
    inlineClassUsageLoweringPhase,

    expressionBodyTransformer,
    eraseVirtualDispatchReceiverParametersTypes,
    bridgesConstructionPhase,

    objectDeclarationLoweringPhase,
    genericReturnTypeLowering,
    unitToVoidLowering,

    // Replace builtins before autoboxing
    builtInsLoweringPhase0,

    autoboxingTransformerPhase,

    objectUsageLoweringPhase,
    purifyObjectInstanceGettersLoweringPhase,

    explicitlyCastExternalTypesPhase,
    typeOperatorLoweringPhase,

    // Clean up built-ins after type operator lowering
    builtInsLoweringPhase,

    virtualDispatchReceiverExtractionPhase,
    invokeStaticInitializersPhase,
    staticMembersLoweringPhase,
    inlineObjectsWithPureInitializationLoweringPhase,
    whenBranchOptimiserLoweringPhase,
    validateIrAfterLowering,
)

val wasmPhases = SameTypeNamedCompilerPhase(
    name = "IrModuleLowering",
    description = "IR module lowering",
    lower = loweringList.toCompilerPhase(),
    actions = DEFAULT_IR_ACTIONS,
    nlevels = 1
)

