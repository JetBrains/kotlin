/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Symbols.Companion.isTypeOfIntrinsic
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.optimizations.PropertyAccessorInlineLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.wasm.lower.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.JsSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.inline.DumpSyntheticAccessors
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.inline.isConsideredAsPrivateForInlining
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.utils.bind

private fun List<CompilerPhase<WasmBackendContext, IrModuleFragment, IrModuleFragment>>.toCompilerPhase() =
    reduce { acc, lowering -> acc.then(lowering) }

private val validateIrBeforeLowering = makeIrModulePhase(
    ::IrValidationBeforeLoweringPhase,
    name = "ValidateIrBeforeLowering",
)

private val validateIrAfterInliningOnlyPrivateFunctionsPhase = makeIrModulePhase(
    { context: WasmBackendContext ->
        IrValidationAfterInliningOnlyPrivateFunctionsPhase(
            context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-69457:
                    inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                    // Call sites of only non-private functions are allowed at this stage.
                    else -> !inlineFunction.isConsideredAsPrivateForInlining()
                }
            }
        )
    },
    name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase",
)

private val validateIrAfterInliningAllFunctionsPhase = makeIrModulePhase(
    { context: WasmBackendContext ->
        IrValidationAfterInliningAllFunctionsPhase(
            context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                when {
                    // TODO: remove this condition after the fix of KT-69457:
                    inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                    // TODO: remove this condition after the fix of KT-70361:
                    isTypeOfIntrinsic(inlineFunction.symbol) -> true // temporarily permitted

                    else -> false // forbidden
                }
            }
        )
    },
    name = "IrValidationAfterInliningAllFunctionsPhase",
)

private val dumpSyntheticAccessorsPhase = makeIrModulePhase<WasmBackendContext>(
    ::DumpSyntheticAccessors,
    name = "DumpSyntheticAccessorsPhase",
)

private val validateIrAfterLowering = makeIrModulePhase(
    ::IrValidationAfterLoweringPhase,
    name = "ValidateIrAfterLowering",
)

private val generateTests = makeIrModulePhase(
    ::GenerateWasmTests,
    name = "GenerateTests",
)

private val generateTestsIC = makeIrModulePhase(
    ::GenerateWasmTestsIC,
    name = "GenerateTestsIC",
)

private val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
)

private val stringConcatenationLowering = makeIrModulePhase(
    ::StringConcatenationLowering,
    name = "StringConcatenation",
)

private val lateinitPhase = makeIrModulePhase(
    ::LateinitLowering,
    name = "LateinitLowering",
)

private val rangeContainsLoweringPhase = makeIrModulePhase(
    ::RangeContainsLowering,
    name = "RangeContainsLowering",
)

private val inlineCallableReferenceToLambdaPhase = makeIrModulePhase(
    ::WasmInlineCallableReferenceToLambdaPhase,
    name = "WasmInlineCallableReferenceToLambdaPhase",
)


private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = makeIrModulePhase(
    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "WrapInlineDeclarationsWithReifiedTypeParametersLowering",
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    prerequisite = setOf(inlineCallableReferenceToLambdaPhase)
)

private val sharedVariablesLoweringPhase = makeIrModulePhase(
    { context: WasmBackendContext -> SharedVariablesLowering(WasmSharedVariablesManager(context.wasmSymbols)) },
    name = "SharedVariablesLowering",
    prerequisite = setOf(lateinitPhase)
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
)

private val localClassesInInlineFunctionsPhase = makeIrModulePhase(
    ::LocalClassesInInlineFunctionsLowering,
    name = "LocalClassesInInlineFunctionsPhase",
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = makeIrModulePhase(
    ::WasmFunctionInlining.bind(InlineMode.PRIVATE_INLINE_FUNCTIONS),
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

internal val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = ::SyntheticAccessorLowering,
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase),
)

private val inlineAllFunctionsPhase = makeIrModulePhase(
    { context: WasmBackendContext ->
        FunctionInlining(
            context,
            WasmInlineFunctionResolver(context, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
            produceOuterThisFields = false,
        )
    },
    name = "InlineAllFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

private val removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase = makeIrModulePhase(
    { RemoveInlineDeclarationsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val tailrecLoweringPhase = makeIrModulePhase(
    ::TailrecLowering,
    name = "TailrecLowering",
)

private val wasmStringSwitchOptimizerLowering = makeIrModulePhase(
    ::WasmStringSwitchOptimizerLowering,
    name = "WasmStringSwitchOptimizerLowering",
)

private val jsCodeCallsLowering = makeIrModulePhase(
    ::JsCodeCallsLowering,
    name = "JsCodeCallsLowering",
)

private val complexExternalDeclarationsToTopLevelFunctionsLowering = makeIrModulePhase(
    ::ComplexExternalDeclarationsToTopLevelFunctionsLowering,
    name = "ComplexExternalDeclarationsToTopLevelFunctionsLowering",
)

private val complexExternalDeclarationsUsagesLowering = makeIrModulePhase(
    ::ComplexExternalDeclarationsUsageLowering,
    name = "ComplexExternalDeclarationsUsageLowering",
)

private val jsInteropFunctionsLowering = makeIrModulePhase(
    ::JsInteropFunctionsLowering,
    name = "JsInteropFunctionsLowering",
)

private val jsInteropFunctionCallsLowering = makeIrModulePhase(
    ::JsInteropFunctionCallsLowering,
    name = "JsInteropFunctionCallsLowering",
)

private val enumWhenPhase = makeIrModulePhase(
    ::EnumWhenLowering,
    name = "EnumWhenLowering",
)

private val enumClassConstructorLoweringPhase = makeIrModulePhase(
    ::EnumClassConstructorLowering,
    name = "EnumClassConstructorLowering",
)

private val enumClassConstructorBodyLoweringPhase = makeIrModulePhase(
    ::EnumClassConstructorBodyTransformer,
    name = "EnumClassConstructorBodyLowering",
)

private val enumEntryInstancesLoweringPhase = makeIrModulePhase(
    ::EnumEntryInstancesLowering,
    name = "EnumEntryInstancesLowering",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryInstancesBodyLoweringPhase = makeIrModulePhase(
    ::EnumEntryInstancesBodyLowering,
    name = "EnumEntryInstancesBodyLowering",
    prerequisite = setOf(enumEntryInstancesLoweringPhase)
)

private val enumClassCreateInitializerLoweringPhase = makeIrModulePhase(
    ::EnumClassCreateInitializerLowering,
    name = "EnumClassCreateInitializerLowering",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumEntryCreateGetInstancesFunsLoweringPhase = makeIrModulePhase(
    ::EnumEntryCreateGetInstancesFunsLowering,
    name = "EnumEntryCreateGetInstancesFunsLowering",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val enumSyntheticFunsLoweringPhase = makeIrModulePhase(
    ::EnumSyntheticFunctionsAndPropertiesLowering,
    name = "EnumSyntheticFunctionsAndPropertiesLowering",
    prerequisite = setOf(
        enumClassConstructorLoweringPhase,
        enumClassCreateInitializerLoweringPhase,
        enumEntryCreateGetInstancesFunsLoweringPhase
    )
)

private val enumUsageLoweringPhase = makeIrModulePhase(
    ::EnumUsageLowering,
    name = "EnumUsageLowering",
    prerequisite = setOf(enumEntryCreateGetInstancesFunsLoweringPhase)
)

private val enumEntryRemovalLoweringPhase = makeIrModulePhase(
    ::EnumClassRemoveEntriesLowering,
    name = "EnumEntryRemovalLowering",
    prerequisite = setOf(enumUsageLoweringPhase)
)


private val propertyReferenceLowering = makeIrModulePhase(
    ::WasmPropertyReferenceLowering,
    name = "WasmPropertyReferenceLowering",
)

private val callableReferencePhase = makeIrModulePhase(
    ::CallableReferenceLowering,
    name = "WasmCallableReferenceLowering",
)

private val singleAbstractMethodPhase = makeIrModulePhase(
    ::JsSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
)

private val localDelegatedPropertiesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { LocalDelegatedPropertiesLowering() },
    name = "LocalDelegatedPropertiesLowering",
)

private val localDeclarationsLoweringPhase = makeIrModulePhase(
    ::LocalDeclarationsLowering,
    name = "LocalDeclarationsLowering",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localClassExtractionPhase = makeIrModulePhase(
    ::LocalClassPopupLowering,
    name = "LocalClassExtractionPhase",
    prerequisite = setOf(localDeclarationsLoweringPhase)
)

private val staticCallableReferenceLoweringPhase = makeIrModulePhase(
    ::WasmStaticCallableReferenceLowering,
    name = "WasmStaticCallableReferenceLowering",
    prerequisite = setOf(callableReferencePhase, localClassExtractionPhase)
)

private val innerClassesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    ::InnerClassesLowering,
    name = "InnerClassesLowering",
)

private val innerClassesMemberBodyLoweringPhase = makeIrModulePhase(
    ::InnerClassesMemberBodyLowering,
    name = "InnerClassesMemberBody",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val innerClassConstructorCallsLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCallsLowering",
)

private val suspendFunctionsLoweringPhase = makeIrModulePhase(
    ::JsSuspendFunctionsLowering,
    name = "SuspendFunctionsLowering",
)

private val addContinuationToNonLocalSuspendFunctionsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    name = "AddContinuationToNonLocalSuspendFunctionsLowering",
)

private val addContinuationToFunctionCallsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToFunctionCallsLowering,
    name = "AddContinuationToFunctionCallsLowering",
    prerequisite = setOf(
        addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    )
)

private val generateMainFunctionWrappersPhase = makeIrModulePhase(
    ::GenerateMainFunctionWrappers,
    name = "GenerateMainFunctionWrappers",
)

private val defaultArgumentStubGeneratorPhase = makeIrModulePhase<WasmBackendContext>(
    { context -> DefaultArgumentStubGenerator(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) },
    name = "DefaultArgumentStubGenerator",
)

private val defaultArgumentPatchOverridesPhase = makeIrModulePhase(
    ::DefaultParameterPatchOverridenSymbolsLowering,
    name = "DefaultArgumentsPatchOverrides",
    prerequisite = setOf(defaultArgumentStubGeneratorPhase)
)

private val defaultParameterInjectorPhase = makeIrModulePhase(
    { context -> DefaultParameterInjector(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) },
    name = "DefaultParameterInjector",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val defaultParameterCleanerPhase = makeIrModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
)

private val propertiesLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { PropertiesLowering() },
    name = "PropertiesLowering",
)

private val primaryConstructorLoweringPhase = makeIrModulePhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
)

private val delegateToPrimaryConstructorLoweringPhase = makeIrModulePhase(
    ::DelegateToSyntheticPrimaryConstructor,
    name = "DelegateToSyntheticPrimaryConstructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val initializersLoweringPhase = makeIrModulePhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    prerequisite = setOf(primaryConstructorLoweringPhase, localClassExtractionPhase)
)

private val initializersCleanupLoweringPhase = makeIrModulePhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    prerequisite = setOf(initializersLoweringPhase)
)

private val excludeDeclarationsFromCodegenPhase = makeIrModulePhase(
    ::ExcludeDeclarationsFromCodegen,
    name = "ExcludeDeclarationsFromCodegen",
)

private val unhandledExceptionLowering = makeIrModulePhase(
    ::UnhandledExceptionLowering,
    name = "UnhandledExceptionLowering",
)

private val tryCatchCanonicalization = makeIrModulePhase(
    ::TryCatchCanonicalization,
    name = "TryCatchCanonicalization",
    prerequisite = setOf(inlineAllFunctionsPhase, unhandledExceptionLowering)
)

private val bridgesConstructionPhase = makeIrModulePhase(
    ::WasmBridgesConstruction,
    name = "BridgesConstruction",
)

private val inlineClassDeclarationLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { InlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
)

private val inlineClassUsageLoweringPhase = makeIrModulePhase<WasmBackendContext>(
    { InlineClassLowering(it).inlineClassUsageLowering },
    name = "InlineClassUsageLowering",
)

private val autoboxingTransformerPhase = makeIrModulePhase<WasmBackendContext>(
    { context -> AutoboxingTransformer(context) },
    name = "AutoboxingTransformer",
)

private val staticMembersLoweringPhase = makeIrModulePhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
)

private val classReferenceLoweringPhase = makeIrModulePhase(
    ::WasmClassReferenceLowering,
    name = "WasmClassReferenceLowering",
)

private val wasmVarargExpressionLoweringPhase = makeIrModulePhase(
    ::WasmVarargExpressionLowering,
    name = "WasmVarargExpressionLowering",
)

private val builtInsLoweringPhase0 = makeIrModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering0",
)

private val builtInsLoweringPhase = makeIrModulePhase(
    ::BuiltInsLowering,
    name = "BuiltInsLowering",
)

private val associatedObjectsLowering = makeIrModulePhase(
    ::AssociatedObjectsLowering,
    name = "AssociatedObjectsLowering",
    prerequisite = setOf(localClassExtractionPhase)
)

private val objectDeclarationLoweringPhase = makeIrModulePhase(
    ::ObjectDeclarationLowering,
    name = "ObjectDeclarationLowering",
    prerequisite = setOf(enumClassCreateInitializerLoweringPhase, staticCallableReferenceLoweringPhase)
)

private val invokeStaticInitializersPhase = makeIrModulePhase(
    ::InvokeStaticInitializersLowering,
    name = "InvokeStaticInitializersLowering",
    prerequisite = setOf(objectDeclarationLoweringPhase)
)

private val objectUsageLoweringPhase = makeIrModulePhase(
    ::ObjectUsageLowering,
    name = "ObjectUsageLowering",
)

private val explicitlyCastExternalTypesPhase = makeIrModulePhase(
    ::ExplicitlyCastExternalTypesLowering,
    name = "ExplicitlyCastExternalTypesLowering",
)

private val typeOperatorLoweringPhase = makeIrModulePhase(
    ::WasmTypeOperatorLowering,
    name = "TypeOperatorLowering",
)

private val genericReturnTypeLowering = makeIrModulePhase(
    ::GenericReturnTypeLowering,
    name = "GenericReturnTypeLowering",
)

private val eraseVirtualDispatchReceiverParametersTypes = makeIrModulePhase(
    ::EraseVirtualDispatchReceiverParametersTypes,
    name = "EraseVirtualDispatchReceiverParametersTypes",
)

private val virtualDispatchReceiverExtractionPhase = makeIrModulePhase(
    ::VirtualDispatchReceiverExtraction,
    name = "VirtualDispatchReceiverExtraction",
)

private val forLoopsLoweringPhase = makeIrModulePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
)

private val propertyLazyInitLoweringPhase = makeIrModulePhase(
    ::PropertyLazyInitLowering,
    name = "PropertyLazyInitLowering",
)

private val removeInitializersForLazyProperties = makeIrModulePhase(
    ::RemoveInitializersForLazyProperties,
    name = "RemoveInitializersForLazyProperties",
)

private val propertyAccessorInlinerLoweringPhase = makeIrModulePhase(
    ::PropertyAccessorInlineLowering,
    name = "PropertyAccessorInlineLowering",
)

private val invokeOnExportedFunctionExitLowering = makeIrModulePhase(
    ::InvokeOnExportedFunctionExitLowering,
    name = "InvokeOnExportedFunctionExitLowering",
)

private val expressionBodyTransformer = makeIrModulePhase(
    ::ExpressionBodyTransformer,
    name = "ExpressionBodyTransformer",
)

private val unitToVoidLowering = makeIrModulePhase(
    ::UnitToVoidLowering,
    name = "UnitToVoidLowering",
)

private val purifyObjectInstanceGettersLoweringPhase = makeIrModulePhase(
    ::PurifyObjectInstanceGettersLowering,
    name = "PurifyObjectInstanceGettersLowering",
    prerequisite = setOf(objectDeclarationLoweringPhase, objectUsageLoweringPhase)
)

private val inlineObjectsWithPureInitializationLoweringPhase = makeIrModulePhase(
    ::InlineObjectsWithPureInitializationLowering,
    name = "InlineObjectsWithPureInitializationLowering",
    prerequisite = setOf(purifyObjectInstanceGettersLoweringPhase)
)

private val whenBranchOptimiserLoweringPhase = makeIrModulePhase(
    ::WhenBranchOptimiserLowering,
    name = "WhenBranchOptimiserLowering",
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
    prerequisite = setOf(inlineAllFunctionsPhase)
)

fun getWasmLowerings(
    configuration: CompilerConfiguration,
    isIncremental: Boolean
): List<SimpleNamedCompilerPhase<WasmBackendContext, IrModuleFragment, IrModuleFragment>> = listOfNotNull(
    // BEGIN: Common Native/JS/Wasm prefix.
    validateIrBeforeLowering,
    lateinitPhase,
    sharedVariablesLoweringPhase,
    outerThisSpecialAccessorInInlineFunctionsPhase,
    localClassesInInlineLambdasPhase,
    inlineCallableReferenceToLambdaPhase,
    arrayConstructorPhase,
    wrapInlineDeclarationsWithReifiedTypeParametersLowering,
    inlineOnlyPrivateFunctionsPhase,
    syntheticAccessorGenerationPhase,
    // Note: The validation goes after both `inlineOnlyPrivateFunctionsPhase` and `syntheticAccessorGenerationPhase`
    // just because it goes so in Native.
    validateIrAfterInliningOnlyPrivateFunctionsPhase,
    inlineAllFunctionsPhase,
    dumpSyntheticAccessorsPhase.takeIf {
        configuration[KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR] != null
    },
    validateIrAfterInliningAllFunctionsPhase,
    // END: Common Native/JS/Wasm prefix.

    constEvaluationPhase,
    removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,

    jsCodeCallsLowering,

    generateTests.takeIf { !isIncremental },
    generateTestsIC.takeIf { isIncremental },

    excludeDeclarationsFromCodegenPhase,
    expectDeclarationsRemovingPhase,
    rangeContainsLoweringPhase,

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

    // This doesn't work with IC as of now for accessors within inline functions because
    //  there is no special case for Wasm in the computation of inline function transitive
    //  hashes the same way it's being done with the calculation of symbol hashes.
    propertyAccessorInlinerLoweringPhase.takeIf { !isIncremental },

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
    purifyObjectInstanceGettersLoweringPhase.takeIf { !isIncremental },

    explicitlyCastExternalTypesPhase,
    typeOperatorLoweringPhase,

    // Clean up built-ins after type operator lowering
    builtInsLoweringPhase,

    virtualDispatchReceiverExtractionPhase,
    invokeStaticInitializersPhase,
    staticMembersLoweringPhase,

    // This is applied for non-IC mode, which is a better optimization than inlineUnitInstanceGettersLowering
    inlineObjectsWithPureInitializationLoweringPhase.takeIf { !isIncremental },

    whenBranchOptimiserLoweringPhase,
    validateIrAfterLowering,
)

fun getWasmPhases(
    configuration: CompilerConfiguration,
    isIncremental: Boolean
): SameTypeNamedCompilerPhase<WasmBackendContext, IrModuleFragment> = SameTypeNamedCompilerPhase(
    name = "IrModuleLowering",
    lower = getWasmLowerings(configuration, isIncremental).toCompilerPhase(),
    actions = DEFAULT_IR_ACTIONS,
    nlevels = 1
)