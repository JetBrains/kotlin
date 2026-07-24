/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibErrors
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CopyInlineFunctionBodyLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.JsAllFunctionInlining
import org.jetbrains.kotlin.ir.backend.js.lower.inline.JsPrivateFunctionInlining
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.inline.isConsideredAsPrivateForInlining
import org.jetbrains.kotlin.ir.inline.loweringsOfTheFirstPhase
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsic

private fun createValidateIrAfterInliningOnlyPrivateFunctions(context: LoweringContext): IrValidationAfterInliningOnlyPrivateFunctionsPhase<LoweringContext> {
    return IrValidationAfterInliningOnlyPrivateFunctionsPhase(
        context,
        checkInlineFunctionCallSites = { inlineFunctionUseSite ->
            // Call sites of only non-private functions are allowed at this stage.
            !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
        }
    )
}

private fun createValidateIrAfterInliningAllFunctions(context: LoweringContext): IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase<LoweringContext> {
    return IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase(
        context,
        checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
            // No inline function call sites should remain at this stage.
            val inlineFunction = inlineFunctionUseSite.symbol.owner
            // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
            if (inlineFunction.symbol.isTypeOfIntrinsic()) return@check true
            return@check inlineFunction.body == null
        }
    )
}

@Suppress("unused")
private fun createInventNamesForLocalFunctionsPhase(context: JsIrBackendContext): KlibInventNamesForLocalFunctions {
    return KlibInventNamesForLocalFunctions(suggestUniqueNames = false)
}

private fun createKotlinNothingValueExceptionPhase(context: CommonBackendContext): KotlinNothingValueExceptionLowering {
    return KotlinNothingValueExceptionLowering(context)
}

private fun createJsCodeOutliningPhaseOnSecondStage(context: LoweringContext): JsCodeOutliningLowering {
    return JsCodeOutliningLowering(context)
}

private fun createJsCodeOutliningPhaseOnFirstStage(context: JsPreSerializationLoweringContext): JsCodeOutliningLowering {
    return JsCodeOutliningLowering(context) { jsCall, valueDeclaration, container ->
        context.diagnosticReporter.at(jsCall, container)
            .report(JsKlibErrors.JS_CODE_CAPTURES_INLINABLE_FUNCTION, valueDeclaration)
    }
}

private fun createSharedVariablesLoweringPhase(context: LoweringContext): SharedVariablesLowering {
    return SharedVariablesLowering(context)
}

private fun createSyntheticAccessorGenerationPhase(context: LoweringContext): SyntheticAccessorLowering {
    return SyntheticAccessorLowering(context)
}

private fun createUpgradeCallableReferences(context: LoweringContext): UpgradeCallableReferences {
    return UpgradeCallableReferences(
        context,
        upgradeSamConversions = false,
    )
}

private fun createLocalDeclarationsLoweringPhase(context: LoweringContext): LocalDeclarationsLowering {
    return LocalDeclarationsLowering(context)
}

private fun createDefaultParameterCleanerPhase(context: CommonBackendContext): DefaultParameterCleaner {
    return DefaultParameterCleaner(context)
}

private fun createAutoboxingTransformerPhase(context: JsCommonBackendContext): AutoboxingTransformer {
    return AutoboxingTransformer(context, replaceTypesInsideInlinedFunctionBlock = true)
}

private fun createConstEvaluationPhase(context: JsIrBackendContext): ConstEvaluationLowering {
    return ConstEvaluationLowering(context, isFloatingPointOptimizationEnabled = false)
}

fun jsLoweringsOfTheFirstPhase(
    languageVersionSettings: LanguageVersionSettings,
): List<NamedCompilerPhase<JsPreSerializationLoweringContext, IrModuleFragment, IrModuleFragment>> {
    val phases = buildList<(JsPreSerializationLoweringContext) -> ModuleLoweringPass> {
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs)) {
            this += ::createUpgradeCallableReferences
        }
        if (languageVersionSettings.supportsFeature(LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization)) {
            this += ::createJsCodeOutliningPhaseOnFirstStage
        }
        this += loweringsOfTheFirstPhase(languageVersionSettings)
    }
    return createModulePhases(*phases.toTypedArray())
}

val jsLowerings: List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> = createModulePhases(
    // BEGIN: Common Native/JS/Wasm prefix.
    ::KlibIrValidationBeforeLoweringPhase,
    ::InlineCallCycleCheckerLowering,
    ::createUpgradeCallableReferences,
    ::createJsCodeOutliningPhaseOnSecondStage,
    ::LateinitLowering,
    ::createSharedVariablesLoweringPhase,
    ::LocalClassesInInlineLambdasLowering,
    ::ArrayConstructorLowering,
    ::JsPrivateFunctionInlining,
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    ::createSyntheticAccessorGenerationPhase,
    // Note: The validation goes after both `inlineOnlyPrivateFunctionsPhase` and `syntheticAccessorGenerationPhase`
    // just because it goes so in Native.
    ::createValidateIrAfterInliningOnlyPrivateFunctions,
    ::JsAllFunctionInlining,
    ::RedundantCastsRemoverLowering,
    ::createValidateIrAfterInliningAllFunctions,
    // END: Common Native/JS/Wasm prefix.

    ::createConstEvaluationPhase,
    ::CopyInlineFunctionBodyLowering,
    ::RemoveInlineDeclarationsWithReifiedTypeParametersLowering,
    ::PrepareInlineClassesToBeExportedLowering,
    ::PrepareExportedDefaultImplementationsLowering,
    ::ReplaceSuspendIntrinsicLowering,
    ::PrepareSuspendFunctionsForExportLowering,
    ::ReplaceExportedSuspendFunctionsCallsWithTheirBridgeCall,
    ::IgnoreOriginalSuspendFunctionsThatWereExportedLowering,
    ::ImplicitlyExportedDeclarationsMarkingLowering,
    ::ExcludeSyntheticDeclarationsFromExportLowering,
    ::JsStaticLowering,
    ::JsInventNamesForLocalClasses,
    ::JsCollectClassIdentifiersLowering,
    ::JsCommonAnnotationImplementationTransformer,
    ::ExpectDeclarationsRemoveLowering,
    ::StripTypeAliasDeclarationsLowering,
    ::CreateScriptFunctionsPhase,
    ::JsStringConcatenationLowering,
    ::PropertyReferenceLowering,
    ::JsCallableReferenceLowering,
    ::JsSingleAbstractMethodLowering,
    ::TailrecLowering,
    ::EnumClassConstructorLowering,
    ::EnumClassConstructorBodyTransformer,
    ::LocalDelegatedPropertiesLowering,
    ::createInventNamesForLocalFunctionsPhase,
    ::createLocalDeclarationsLoweringPhase,
    ::LocalDeclarationPopupLowering,
    ::InnerClassesLowering,
    ::InnerClassesMemberBodyLowering,
    ::InnerClassConstructorCallsLowering,
    ::JsClassUsageInReflectionLowering,
    ::PropertiesLowering,
    ::PrimaryConstructorLowering,
    ::DelegateToSyntheticPrimaryConstructor,
    ::AnnotationConstructorLowering,
    ::EnumEntryInstancesLowering,
    ::EnumEntryInstancesBodyLowering,
    ::EnumEntryCreateGetInstancesFunsLowering,
    ::EnumSyntheticFunctionsAndPropertiesLowering,
    ::ObjectDeclarationLowering,
    ::JsStaticInitializersDeclarationLowering,
    ::JsStaticInitializersUsageLowering,
    ::JsInitializersLowering,
    ::JsInitializersCleanupLowering,
    ::createKotlinNothingValueExceptionPhase,
    ::CollectClassDefaultConstructorsLowering,
    ::EnumWhenLowering,
    ::EnumUsageLowering,
    ::ExternalEnumUsagesLowering,
    ::EnumClassRemoveEntriesLowering,
    ::JsSuspendFunctionWithGeneratorsLowering,
    ::JsSuspendFunctionsLowering,
    ::InteropCallableReferenceLowering,
    ::JsSuspendArityStoreLowering,
    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    ::AddContinuationToLocalSuspendFunctionsLowering,
    ::JsAddContinuationToFunctionCallsLowering,
    ::JsReturnableBlockLowering,
    ::RangeContainsLowering,
    ::ForLoopsLowering,
    ::PrimitiveCompanionLowering,
    ::PropertyLazyInitLowering,
    ::RemoveInitializersForLazyProperties,
    ::JsPropertyAccessorInlineLowering,
    ::CopyAccessorBodyLowerings,
    ::BooleanPropertyInExternalLowering,
    ::ExternalPropertyOverridingLowering,
    ::PrivateMembersLowering,
    ::PrivateMemberBodiesLowering,
    ::JsDefaultArgumentStubGenerator,
    ::JsDefaultParameterPatchOverridenSymbolsLowering,
    ::JsDefaultParameterInjector,
    ::createDefaultParameterCleanerPhase,
    ::CaptureStackTraceInThrowables,
    ::ThrowableLowering,
    ::VarargLowering,
    ::MultipleCatchesLowering,
    ::JsBridgesConstruction,
    ::TypeOperatorLowering,
    ::SecondaryConstructorLowering,
    ::SecondaryFactoryInjectorLowering,
    ::JsClassReferenceLowering,
    ::ConstLowering,
    ::JsInlineClassDeclarationLowering,
    ::JsInlineClassUsageLowering,
    ::ExpressionBodyTransformer,
    ::createAutoboxingTransformerPhase,
    ::AutoboxingForExportedInlineClassesForExternalsLowering,
    ::JsBlockDecomposerLowering,
    ::InvokeStaticInitializersLowering,
    ::ObjectUsageLowering,
    ::ES6AddBoxParameterToConstructorsLowering,
    ::ES6SyntheticPrimaryConstructorLowering,
    ::ES6ConstructorLowering,
    ::ES6ConstructorCallLowering,
    ::CallsLowering,
    ::EscapedIdentifiersLowering,
    ::MainFunctionCallWrapperLowering,
    ::CleanupLowering,
    ::IrValidationAfterLoweringPhase,
)

val optimizationLoweringList: List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> = createModulePhases(
    ::ES6CollectConstructorsWhichNeedBoxParameters,
    ::ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering,
    ::ES6ConstructorBoxParameterOptimizationLowering,
    ::ES6PrimaryConstructorOptimizationLowering,
    ::ES6PrimaryConstructorUsageOptimizationLowering,
    ::PurifyObjectInstanceGettersLowering,
    ::InlineObjectsWithPureInitializationLowering,
    ::MoveCallableFactoriesToDeclarationsLowering,
    ::DeduplicateCallableReferenceFactoriesLowering
)
