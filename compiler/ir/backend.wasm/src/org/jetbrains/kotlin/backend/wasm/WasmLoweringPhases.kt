/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.wasm.lower.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.JsSuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

private fun createValidateIrAfterInliningOnlyPrivateFunctionsPhase(context: LoweringContext): IrValidationAfterInliningOnlyPrivateFunctionsPhase<*> {
    return IrValidationAfterInliningOnlyPrivateFunctionsPhase(
        context,
        checkInlineFunctionCallSites = { inlineFunctionUseSite ->
            // Call sites of only non-private functions are allowed at this stage.
            !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
        }
    )
}

private fun createValidateIrAfterInliningAllFunctionsPhase(context: LoweringContext): IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase<*> {
    return IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase(
        context,
        checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
            // No inline function call sites should remain at this stage.
            val inlineFunction = inlineFunctionUseSite.symbol.owner
            // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
            if (PreSerializationSymbols.isTypeOfIntrinsic(inlineFunction.symbol)) return@check true
            return@check inlineFunction.body == null
        }
    )
}

private fun createKotlinNothingValueExceptionPhase(context: CommonBackendContext): KotlinNothingValueExceptionLowering {
    return KotlinNothingValueExceptionLowering(context)
}

private fun createSharedVariablesLoweringPhase(context: LoweringContext): SharedVariablesLowering {
    return SharedVariablesLowering(context)
}

private fun createSpecializeSharedVariableBoxesPhase(context: WasmBackendContext): SharedVariablesPrimitiveBoxSpecializationLowering {
    return SharedVariablesPrimitiveBoxSpecializationLowering(context, context.symbols)
}

private fun createSyntheticAccessorGenerationPhase(context: LoweringContext): SyntheticAccessorLowering {
    return SyntheticAccessorLowering(context)
}

private fun createUpgradeCallableReferences(context: LoweringContext): UpgradeCallableReferences {
    return UpgradeCallableReferences(
        context,
        upgradeFunctionReferencesAndLambdas = true,
        upgradePropertyReferences = true,
        upgradeLocalDelegatedPropertyReferences = true,
        upgradeSamConversions = false,
    )
}

private fun createCaptureRichFunctionReferenceLocals(context: LoweringContext): LocalDeclarationsLowering {
    return LocalDeclarationsLowering(
        context,
        considerRichFunctionReferenceInvokeFunctionsAsLocal = true,
    )
}

@Suppress("unused")
private fun createInventNamesForLocalFunctionsPhase(context: LoweringContext): KlibInventNamesForLocalFunctions{
    return KlibInventNamesForLocalFunctions()
}

private fun createLocalDeclarationsLoweringPhase(context: LoweringContext): LocalDeclarationsLowering {
    return LocalDeclarationsLowering(context)
}

private fun createDefaultParameterCleanerPhase(context: CommonBackendContext): DefaultParameterCleaner {
    return DefaultParameterCleaner(context)
}

private fun createAutoboxingTransformerPhase(context: JsCommonBackendContext): AutoboxingTransformer {
    return AutoboxingTransformer(context)
}

//@PhasePrerequisites(FunctionInlining::class) // This prerequisite is hard to represent for common lowering
private fun createConstEvaluationPhase(context: CommonBackendContext): ConstEvaluationLowering {
    val configuration = IrInterpreterConfiguration(
        printOnlyExceptionMessage = true,
        platform = WasmPlatforms.unspecifiedWasmPlatform,
    )
    return ConstEvaluationLowering(context, configuration = configuration)
}

fun wasmLoweringsOfTheFirstPhase(
    languageVersionSettings: LanguageVersionSettings,
): List<NamedCompilerPhase<WasmPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
    val phases = buildList<(WasmPreSerializationLoweringContext) -> ModuleLoweringPass> {
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs)) {
            this += ::createUpgradeCallableReferences
        }
        this += loweringsOfTheFirstPhase(languageVersionSettings)
    }
    return createModulePhases(*phases.toTypedArray())
}

val wasmLowerings: List<NamedCompilerPhase<WasmBackendContext, IrModuleFragment, IrModuleFragment>> = createModulePhases(
    // BEGIN: Common Native/JS/Wasm prefix.
    ::KlibIrValidationBeforeLoweringPhase,
    ::InlineCallCycleCheckerLowering,
    ::createUpgradeCallableReferences,
    ::LateinitLowering,
    ::createSharedVariablesLoweringPhase,
    ::LocalClassesInInlineLambdasLowering,
    ::ArrayConstructorLowering,
    ::WasmPrivateFunctionInlining,
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    ::createSyntheticAccessorGenerationPhase,
    // Note: The validation goes after both `inlineOnlyPrivateFunctionsPhase` and `syntheticAccessorGenerationPhase`
    // just because it goes so in Native.
    ::createValidateIrAfterInliningOnlyPrivateFunctionsPhase,
    ::WasmAllFunctionInlining,
    ::RedundantCastsRemoverLowering,
    ::createValidateIrAfterInliningAllFunctionsPhase,
    // END: Common Native/JS/Wasm prefix.

    ::createConstEvaluationPhase,
    ::createSpecializeSharedVariableBoxesPhase,
    ::RemoveInlineDeclarationsWithReifiedTypeParametersLowering,

    ::JsCodeCallsLowering,

    ::GenerateWasmTests,

    ::JsCommonAnnotationImplementationTransformer,

    ::ExcludeDeclarationsFromCodegen,
    ::ExpectDeclarationsRemoveLowering,
    ::RangeContainsLowering,

    ::TailrecLowering,

    ::EnumWhenLowering,
    ::EnumClassConstructorLowering,
    ::EnumClassConstructorBodyTransformer,
    ::EnumEntryInstancesLowering,
    ::EnumEntryInstancesBodyLowering,
    ::EnumClassCreateInitializerLowering,
    ::EnumEntryCreateGetInstancesFunsLowering,
    ::EnumSyntheticFunctionsAndPropertiesLowering,

    ::DelegatedPropertyOptimizationLowering,
    ::WasmPropertyReferenceLowering,

    ::JsSingleAbstractMethodLowering,
    ::LocalDelegatedPropertiesLowering,
    ::createCaptureRichFunctionReferenceLocals,
    ::WasmCallableReferenceLowering,
    ::createInventNamesForLocalFunctionsPhase,
    ::createLocalDeclarationsLoweringPhase,
    ::LocalDeclarationPopupLowering,
    ::InnerClassesLowering,
    ::InnerClassesMemberBodyLowering,
    ::InnerClassConstructorCallsLowering,
    ::PropertiesLowering,
    ::PrimaryConstructorLowering,
    ::DelegateToSyntheticPrimaryConstructor,

    ::WasmStringSwitchOptimizerLowering,
    ::AssociatedObjectsLowering,

    ::ComplexExternalDeclarationsToTopLevelFunctionsLowering,
    ::ComplexExternalDeclarationsUsageLowering,

    ::JsInteropFunctionsLowering,

    ::EnumUsageLowering,
    ::EnumClassRemoveEntriesLowering,

    ::JsSuspendFunctionsLowering,
    ::WasmInitializersLowering,
    ::WasmInitializersCleanupLowering,

    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    ::WasmAddContinuationToFunctionCallsLowering,
    ::GenerateMainFunctionWrappers,

    // We need to generate nothing value exceptions after suspend
    // functions have been lowered so that suspend functions
    // declared to return nothing get a chance to get lowered
    // without the exception being inserted.
    ::createKotlinNothingValueExceptionPhase,

    ::InvokeOnExportedFunctionExitLowering,

    ::TryCatchCanonicalization,

    ::ForLoopsLowering,
    ::PropertyLazyInitLowering,
    ::RemoveInitializersForLazyProperties,

    // This doesn't work with IC as of now for accessors within inline functions because
    //  there is no special case for Wasm in the computation of inline function transitive
    //  hashes the same way it's being done with the calculation of symbol hashes.
    ::WasmPropertyAccessorInlineLowering,

    ::WasmStringConcatenationLowering,

    ::WasmDefaultArgumentStubGenerator,
    ::WasmDefaultParameterPatchOverridenSymbolsLowering,
    ::WasmDefaultParameterInjector,
    ::createDefaultParameterCleanerPhase,

//            TODO:
//            multipleCatchesLoweringPhase,
    ::WasmClassReferenceLowering,

    ::WasmVarargExpressionLowering,
    ::InlineClassDeclarationLowering,
    ::InlineClassUsageLowering,

    ::ExpressionBodyTransformer,
    ::EraseVirtualDispatchReceiverParametersTypes,
    ::WasmBridgesConstruction,

    ::ObjectDeclarationLowering, // Also depends on `WasmStaticCallableReferenceLowering`, but it is hard to represent in the common phase
    ::GenericReturnTypeLowering,
    ::UnitToVoidLowering,

    // Replace builtins before autoboxing
    ::BuiltInsLowering,

    ::createAutoboxingTransformerPhase,

    ::ObjectUsageLowering,
    ::WasmPurifyObjectInstanceGettersLowering,

    ::FieldInitializersLowering,

    ::ExplicitlyCastExternalTypesLowering,
    ::WasmTypeOperatorLowering,

    // Clean up built-ins after type operator lowering
    ::BuiltInsLowering,

    ::VirtualDispatchReceiverExtraction,
    ::InvokeStaticInitializersLowering,
    ::StaticMembersLowering,

    // This is applied for non-IC mode, which is a better optimization than inlineUnitInstanceGettersLowering
    ::WasmInlineObjectsWithPureInitializationLowering,

    ::WhenBranchOptimiserLowering,
    ::IrValidationAfterLoweringPhase,
)
