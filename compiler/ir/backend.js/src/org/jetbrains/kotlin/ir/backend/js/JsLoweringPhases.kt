/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.common.lower.PropertiesLowering
import org.jetbrains.kotlin.backend.common.lower.StripTypeAliasDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibErrors
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.CopyInlineFunctionBodyLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.JsInlineFunctionResolver
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.js.JsPlatforms
import kotlin.collections.plusAssign

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
            if (PreSerializationSymbols.isTypeOfIntrinsic(inlineFunction.symbol)) return@check true
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
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        context.diagnosticReporter.deduplicating(),
        context.configuration.languageVersionSettings
    )

    return JsCodeOutliningLowering(context) { jsCall, valueDeclaration, container ->
        irDiagnosticReporter.at(jsCall, container)
            .report(JsKlibErrors.JS_CODE_CAPTURES_INLINABLE_FUNCTION, valueDeclaration)
    }
}

/**
 * The first phase of inlining (inline only private functions).
 */
private fun createInlineOnlyPrivateFunctionsPhase(context: JsIrBackendContext): FunctionInlining {
    return FunctionInlining(
        context,
        JsInlineFunctionResolver(context, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
    )
}

private fun createSyntheticAccessorGenerationPhase(context: LoweringContext): SyntheticAccessorLowering {
    return SyntheticAccessorLowering(context)
}

/**
 * The second phase of inlining (inline all functions).
 */
private fun createInlineAllFunctionsPhase(context: LoweringContext): FunctionInlining {
    return FunctionInlining(
        context,
        JsInlineFunctionResolver(context, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
    )
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

private fun createLocalDeclarationsLoweringPhase(context: LoweringContext): LocalDeclarationsLowering {
    return LocalDeclarationsLowering(context)
}

private fun createDefaultParameterCleanerPhase(context: CommonBackendContext): DefaultParameterCleaner {
    return DefaultParameterCleaner(context)
}

private fun createInlineClassDeclarationLoweringPhase(context: CommonBackendContext): InlineClassLowering.InlineClassDeclarationLowering {
    return InlineClassLowering(context).inlineClassDeclarationLowering
}

// Const lowering generates inline class constructors for unsigned integers which should be lowered by this lowering
// This annotation must be placed on `inlineClassUsageLowering` but it is hard to achieve because this lowering is inner inside class in the common module
//@PhasePrerequisites(ConstLowering::class)
private fun createInlineClassUsageLoweringPhase(context: CommonBackendContext): InlineClassLowering.InlineClassUsageLowering {
    return InlineClassLowering(context).inlineClassUsageLowering
}

private fun createAutoboxingTransformerPhase(context: JsCommonBackendContext): AutoboxingTransformer {
    return AutoboxingTransformer(context, replaceTypesInsideInlinedFunctionBlock = true)
}

private fun createConstEvaluationPhase(context: JsIrBackendContext): ConstEvaluationLowering {
    val configuration = IrInterpreterConfiguration(
        printOnlyExceptionMessage = true,
        platform = JsPlatforms.defaultJsPlatform,
    )
    return ConstEvaluationLowering(context, configuration = configuration)
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
        this += loweringsOfTheFirstPhase(JsManglerIr, languageVersionSettings)
    }
    return createModulePhases(*phases.toTypedArray())
}

fun getJsLowerings(): List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> {
    val phases = listOfNotNull<(JsIrBackendContext) -> ModuleLoweringPass>(
        // BEGIN: Common Native/JS/Wasm prefix.
        ::KlibIrValidationBeforeLoweringPhase,
        ::createUpgradeCallableReferences,
        ::createJsCodeOutliningPhaseOnSecondStage,
        ::LateinitLowering,
        ::SharedVariablesLowering,
        ::LocalClassesInInlineLambdasLowering,
        ::ArrayConstructorLowering,
        ::createInlineOnlyPrivateFunctionsPhase,
        ::OuterThisInInlineFunctionsSpecialAccessorLowering,
        ::createSyntheticAccessorGenerationPhase,
        // Note: The validation goes after both `inlineOnlyPrivateFunctionsPhase` and `syntheticAccessorGenerationPhase`
        // just because it goes so in Native.
        ::createValidateIrAfterInliningOnlyPrivateFunctions,
        ::createInlineAllFunctionsPhase,
        ::createValidateIrAfterInliningAllFunctions,
        // END: Common Native/JS/Wasm prefix.

        ::createConstEvaluationPhase,
        ::CopyInlineFunctionBodyLowering,
        ::RemoveInlineDeclarationsWithReifiedTypeParametersLowering,
        ::ReplaceSuspendIntrinsicLowering,
        ::PrepareSuspendFunctionsForExportLowering,
        ::ReplaceExportedSuspendFunctionsCallsWithTheirBridgeCall,
        ::IgnoreOriginalSuspendFunctionsThatWereExportedLowering,
        ::PrepareCollectionsToExportLowering,
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
        ::JsInitializersLowering,
        ::JsInitializersCleanupLowering,
        ::createKotlinNothingValueExceptionPhase,
        ::CollectClassDefaultConstructorsLowering,
        ::EnumWhenLowering,
        ::EnumEntryInstancesLowering,
        ::EnumEntryInstancesBodyLowering,
        ::EnumClassCreateInitializerLowering,
        ::EnumEntryCreateGetInstancesFunsLowering,
        ::EnumSyntheticFunctionsAndPropertiesLowering,
        ::EnumUsageLowering,
        ::ExternalEnumUsagesLowering,
        ::EnumClassRemoveEntriesLowering,
        ::JsSuspendFunctionsLoweringSelector,
        ::InteropCallableReferenceLowering,
        ::JsSuspendArityStoreLowering,
        ::AddContinuationToNonLocalSuspendFunctionsLowering,
        ::AddContinuationToLocalSuspendFunctionsLowering,
        ::AddContinuationToFunctionCallsLowering,
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
        ::createInlineClassDeclarationLoweringPhase,
        ::createInlineClassUsageLoweringPhase,
        ::ExpressionBodyTransformer,
        ::createAutoboxingTransformerPhase,
        ::ObjectDeclarationLowering,
        ::JsBlockDecomposerLowering,
        ::InvokeStaticInitializersLowering,
        ::ObjectUsageLowering,
        ::ES6AddBoxParameterToConstructorsLowering,
        ::ES6SyntheticPrimaryConstructorLowering,
        ::ES6ConstructorLowering,
        ::ES6ConstructorCallLowering,
        ::CallsLowering,
        ::EscapedIdentifiersLowering,
        ::RemoveImplicitExportsFromCollections,
        ::MainFunctionCallWrapperLowering,
        ::CleanupLowering,
        ::IrValidationAfterLoweringPhase,
    )
    return createModulePhases(*phases.toTypedArray())
}

val optimizationLoweringList: List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> by lazy {
    val phases = listOf<(JsIrBackendContext) -> ModuleLoweringPass>(
        ::ES6CollectConstructorsWhichNeedBoxParameters,
        ::ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering,
        ::ES6ConstructorBoxParameterOptimizationLowering,
        ::ES6PrimaryConstructorOptimizationLowering,
        ::ES6PrimaryConstructorUsageOptimizationLowering,
        ::PurifyObjectInstanceGettersLowering,
        ::InlineObjectsWithPureInitializationLowering,
    )
    createModulePhases(*phases.toTypedArray())
}
