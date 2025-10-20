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
import org.jetbrains.kotlin.backend.common.lower.inline.InlineCallCycleCheckerLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
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
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.js.JsPlatforms

private val validateIrBeforeLowering = makeIrModulePhase(
    ::KlibIrValidationBeforeLoweringPhase,
    name = "ValidateIrBeforeLowering",
)

private val checkInlineCallCyclesPhase = makeIrModulePhase(
    ::InlineCallCycleCheckerLowering,
    name = "InlineCallCycleChecker"
)

private fun createValidateIrAfterInliningOnlyPrivateFunctions(context: LoweringContext): IrValidationAfterInliningOnlyPrivateFunctionsPhase<LoweringContext> {
    return IrValidationAfterInliningOnlyPrivateFunctionsPhase(
        context,
        checkInlineFunctionCallSites = { inlineFunctionUseSite ->
            // Call sites of only non-private functions are allowed at this stage.
            !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
        }
    )
}

private val validateIrAfterInliningOnlyPrivateFunctions = makeIrModulePhase(
    { context: LoweringContext ->
        IrValidationAfterInliningOnlyPrivateFunctionsPhase(
            context,
            checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                // Call sites of only non-private functions are allowed at this stage.
                !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
            }
        )
    },
    name = "IrValidationAfterInliningOnlyPrivateFunctionsPhase",
)

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

private val validateIrAfterInliningAllFunctions = makeIrModulePhase(
    { context: LoweringContext ->
        IrValidationAfterInliningAllFunctionsOnTheSecondStagePhase(
            context,
            checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
                // No inline function call sites should remain at this stage.
                val inlineFunction = inlineFunctionUseSite.symbol.owner
                // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
                if (PreSerializationSymbols.isTypeOfIntrinsic(inlineFunction.symbol)) return@check true
                return@check inlineFunction.body == null
            }
        )
    },
    name = "IrValidationAfterInliningAllFunctionsPhase",
)

private val validateIrAfterLowering = makeIrModulePhase(
    ::IrValidationAfterLoweringPhase,
    name = "ValidateIrAfterLowering",
)

private val collectClassDefaultConstructorsPhase = makeIrModulePhase(
    ::CollectClassDefaultConstructorsLowering,
    name = "CollectClassDefaultConstructorsLowering",
)

private val prepareCollectionsToExportLowering = makeIrModulePhase(
    ::PrepareCollectionsToExportLowering,
    name = "PrepareCollectionsToExportLowering",
)

private val removeImplicitExportsFromCollections = makeIrModulePhase(
    ::RemoveImplicitExportsFromCollections,
    name = "RemoveImplicitExportsFromCollections",
)

private val jsStaticLowering = makeIrModulePhase(
    ::JsStaticLowering,
    name = "JsStaticLowering",
)

val createScriptFunctionsPhase = makeIrModulePhase(
    ::CreateScriptFunctionsPhase,
    name = "CreateScriptFunctionsPhase",
)

private val collectClassIdentifiersLowering = makeIrModulePhase(
    ::JsCollectClassIdentifiersLowering,
    name = "CollectClassIdentifiersLowering",
)

private val inventNamesForLocalClassesPhase = makeIrModulePhase(
    ::JsInventNamesForLocalClasses,
    name = "InventNamesForLocalClasses",
)

private fun createInventNamesForLocalFunctionsPhase(context: JsIrBackendContext): KlibInventNamesForLocalFunctions {
    return KlibInventNamesForLocalFunctions(suggestUniqueNames = false)
}

private val inventNamesForLocalFunctionsPhase = makeIrModulePhase(
    { _: JsIrBackendContext -> KlibInventNamesForLocalFunctions(suggestUniqueNames = false) },
    name = "InventNamesForLocalFunctions",
)

private val annotationInstantiationLowering = makeIrModulePhase(
    ::JsCommonAnnotationImplementationTransformer,
    name = "AnnotationImplementation",
)

private val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoving",
)

private val stringConcatenationLoweringPhase = makeIrModulePhase(
    ::JsStringConcatenationLowering,
    name = "JsStringConcatenationLowering",
)

private val lateinitPhase = makeIrModulePhase(
    ::LateinitLowering,
    name = "LateinitLowering",
)

private fun createKotlinNothingValueExceptionPhase(context: CommonBackendContext): KotlinNothingValueExceptionLowering {
    return KotlinNothingValueExceptionLowering(context)
}

private val kotlinNothingValueExceptionPhase = makeIrModulePhase(
    ::KotlinNothingValueExceptionLowering,
    name = "KotlinNothingValueException",
)

private val stripTypeAliasDeclarationsPhase = makeIrModulePhase<JsIrBackendContext>(
    ::StripTypeAliasDeclarationsLowering,
    name = "StripTypeAliasDeclarations",
)

private fun createJsCodeOutliningPhaseOnSecondStage(context: LoweringContext): JsCodeOutliningLowering {
    return JsCodeOutliningLowering(context)
}

private val jsCodeOutliningPhaseOnSecondStage = makeIrModulePhase(
    { context: JsIrBackendContext -> JsCodeOutliningLowering(context) },
    name = "JsCodeOutliningLoweringOnSecondStage",
)

private fun createJsCodeOutliningPhaseOnFirstStage(context: JsPreSerializationLoweringContext): JsCodeOutliningLowering {
    return JsCodeOutliningLowering(context) { jsCall, valueDeclaration, container ->
        context.diagnosticReporter.at(jsCall, container)
            .report(JsKlibErrors.JS_CODE_CAPTURES_INLINABLE_FUNCTION, valueDeclaration)
    }
}

private val jsCodeOutliningPhaseOnFirstStage = makeIrModulePhase(
    { context: JsPreSerializationLoweringContext ->
        JsCodeOutliningLowering(context) { jsCall, valueDeclaration, container ->
            context.diagnosticReporter.at(jsCall, container)
                .report(JsKlibErrors.JS_CODE_CAPTURES_INLINABLE_FUNCTION, valueDeclaration)
        }
    },
    name = "JsCodeOutliningLoweringOnFirstStage",
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
)

private fun createSharedVariablesLoweringPhase(context: LoweringContext): SharedVariablesLowering {
    return SharedVariablesLowering(context)
}

private val sharedVariablesLoweringPhase = makeIrModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    prerequisite = setOf(lateinitPhase)
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
)

private val replaceSuspendIntrinsicLowering = makeIrModulePhase(
    ::ReplaceSuspendIntrinsicLowering,
    name = "ReplaceSuspendIntrinsicLowering",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = makeIrModulePhase(
    ::JsPrivateFunctionInlining,
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(arrayConstructorPhase)
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase)
)

private fun createSyntheticAccessorGenerationPhase(context: LoweringContext): SyntheticAccessorLowering {
    return SyntheticAccessorLowering(context)
}

private val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = ::SyntheticAccessorLowering,
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase),
)

/**
 * The second phase of inlining (inline all functions).
 */
private val inlineAllFunctionsPhase = makeIrModulePhase(
    ::JsAllFunctionInlining,
    name = "InlineAllFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

private val redundantCastsRemoverPhase = makeIrModulePhase(
    lowering = ::RedundantCastsRemoverLowering,
    name = "RedundantCastsRemoverLowering",
    prerequisite = setOf(inlineAllFunctionsPhase),
)

private val copyInlineFunctionBodyLoweringPhase = makeIrModulePhase(
    ::CopyInlineFunctionBodyLowering,
    name = "CopyInlineFunctionBody",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase = makeIrModulePhase(
    ::RemoveInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val captureStackTraceInThrowablesPhase = makeIrModulePhase(
    ::CaptureStackTraceInThrowables,
    name = "CaptureStackTraceInThrowables",
)

private val throwableSuccessorsLoweringPhase = makeIrModulePhase(
    ::ThrowableLowering,
    name = "ThrowableLowering",
    prerequisite = setOf(captureStackTraceInThrowablesPhase)
)

private val tailrecLoweringPhase = makeIrModulePhase(
    ::TailrecLowering,
    name = "TailrecLowering",
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
        enumEntryCreateGetInstancesFunsLoweringPhase,
    )
)

private val enumUsageLoweringPhase = makeIrModulePhase(
    ::EnumUsageLowering,
    name = "EnumUsageLowering",
    prerequisite = setOf(enumEntryCreateGetInstancesFunsLoweringPhase)
)

private val externalEnumUsageLoweringPhase = makeIrModulePhase(
    ::ExternalEnumUsagesLowering,
    name = "ExternalEnumUsagesLowering",
)

private val enumEntryRemovalLoweringPhase = makeIrModulePhase(
    ::EnumClassRemoveEntriesLowering,
    name = "EnumEntryRemovalLowering",
    prerequisite = setOf(enumUsageLoweringPhase)
)

private fun createUpgradeCallableReferences(context: LoweringContext): UpgradeCallableReferences {
    return UpgradeCallableReferences(
        context,
        upgradeFunctionReferencesAndLambdas = true,
        upgradePropertyReferences = true,
        upgradeLocalDelegatedPropertyReferences = true,
        upgradeSamConversions = false,
    )
}

private val upgradeCallableReferences = makeIrModulePhase(
    { ctx: LoweringContext ->
        UpgradeCallableReferences(
            ctx,
            upgradeFunctionReferencesAndLambdas = true,
            upgradePropertyReferences = true,
            upgradeLocalDelegatedPropertyReferences = true,
            upgradeSamConversions = false,
        )
    },
    name = "UpgradeCallableReferences"
)

private val propertyReferenceLoweringPhase = makeIrModulePhase(
    ::PropertyReferenceLowering,
    name = "PropertyReferenceLowering",
)

private val callableReferenceLowering = makeIrModulePhase(
    ::JsCallableReferenceLowering,
    name = "CallableReferenceLowering",
    prerequisite = setOf(propertyReferenceLoweringPhase, inlineAllFunctionsPhase)
)

private val returnableBlockLoweringPhase = makeIrModulePhase(
    ::JsReturnableBlockLowering,
    name = "JsReturnableBlockLowering",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val rangeContainsLoweringPhase = makeIrModulePhase(
    ::RangeContainsLowering,
    name = "RangeContainsLowering",
)

private val forLoopsLoweringPhase = makeIrModulePhase(
    ::ForLoopsLowering,
    name = "ForLoopsLowering",
)

private val enumWhenPhase = makeIrModulePhase(
    ::EnumWhenLowering,
    name = "EnumWhenLowering",
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
    ::JsPropertyAccessorInlineLowering,
    name = "PropertyAccessorInlineLowering",
)

private val copyPropertyAccessorBodiesLoweringPass = makeIrModulePhase(
    ::CopyAccessorBodyLowerings,
    name = "CopyAccessorBodyLowering",
    prerequisite = setOf(propertyAccessorInlinerLoweringPhase)
)

private val booleanPropertyInExternalLowering = makeIrModulePhase(
    ::BooleanPropertyInExternalLowering,
    name = "BooleanPropertyInExternalLowering",
)

private val localDelegatedPropertiesLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    ::LocalDelegatedPropertiesLowering,
    name = "LocalDelegatedPropertiesLowering",
)

private fun createLocalDeclarationsLoweringPhase(context: LoweringContext): LocalDeclarationsLowering {
    return LocalDeclarationsLowering(context)
}

private val localDeclarationsLoweringPhase = makeIrModulePhase(
    ::LocalDeclarationsLowering,
    name = "LocalDeclarationsLowering",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localDeclarationExtractionPhase = makeIrModulePhase(
    { context -> LocalDeclarationPopupLowering(context) },
    name = "LocalDeclarationExtractionPhase",
    prerequisite = setOf(localDeclarationsLoweringPhase)
)

private val innerClassesLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    ::InnerClassesLowering,
    name = "InnerClassesLowering",
)

private val innerClassesMemberBodyLoweringPhase = makeIrModulePhase(
    ::InnerClassesMemberBodyLowering,
    name = "InnerClassesMemberBody",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val innerClassConstructorCallsLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCallsLowering",
)

private val suspendFunctionsLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    { context ->
        if (context.compileSuspendAsJsGenerator) {
            JsSuspendFunctionWithGeneratorsLowering(context)
        } else {
            JsSuspendFunctionsLowering(context)
        }
    },
    name = "SuspendFunctionsLowering",
)

private val addContinuationToNonLocalSuspendFunctionsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToNonLocalSuspendFunctionsLowering,
    name = "AddContinuationToNonLocalSuspendFunctionsLowering",
)

private val addContinuationToLocalSuspendFunctionsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToLocalSuspendFunctionsLowering,
    name = "AddContinuationToLocalSuspendFunctionsLowering",
)

private val addContinuationToFunctionCallsLoweringPhase = makeIrModulePhase(
    ::AddContinuationToFunctionCallsLowering,
    name = "AddContinuationToFunctionCallsLowering",
    prerequisite = setOf(
        addContinuationToLocalSuspendFunctionsLoweringPhase,
        addContinuationToNonLocalSuspendFunctionsLoweringPhase,
    )
)

private val prepareSuspendFunctionsForExportLowering = makeIrModulePhase(
    ::PrepareSuspendFunctionsForExportLowering,
    name = "PrepareSuspendFunctionsForExportLowering",
)

private val replaceExportedSuspendFunctionCallsWithItsBridge = makeIrModulePhase(
    ::ReplaceExportedSuspendFunctionsCallsWithTheirBridgeCall,
    name = "ReplaceExportedSuspendFunctionsCallsWithTheirBridgeCall",
    prerequisite = setOf(prepareSuspendFunctionsForExportLowering)
)

private val ignoreOriginalSuspendFunctionsThatWereExportedLowering = makeIrModulePhase(
    ::IgnoreOriginalSuspendFunctionsThatWereExportedLowering,
    name = "IgnoreOriginalSuspendFunctionsThatWereExportedLowering",
    prerequisite = setOf(prepareSuspendFunctionsForExportLowering, replaceExportedSuspendFunctionCallsWithItsBridge)
)

private val implicitlyExportedDeclarationsMarkingLowering = makeIrModulePhase(
    ::ImplicitlyExportedDeclarationsMarkingLowering,
    name = "ImplicitlyExportedDeclarationsMarkingLowering",
    prerequisite = setOf(prepareSuspendFunctionsForExportLowering, prepareCollectionsToExportLowering)
)

private val preventExportOfSyntheticDeclarationsLowering = makeIrModulePhase(
    ::ExcludeSyntheticDeclarationsFromExportLowering,
    name = "ExcludeSyntheticDeclarationsFromExportLowering",
    prerequisite = setOf(implicitlyExportedDeclarationsMarkingLowering)
)

private val privateMembersLoweringPhase = makeIrModulePhase(
    ::PrivateMembersLowering,
    name = "PrivateMembersLowering",
)

private val privateMemberUsagesLoweringPhase = makeIrModulePhase(
    ::PrivateMemberBodiesLowering,
    name = "PrivateMemberUsagesLowering",
)

private val interopCallableReferenceLoweringPhase = makeIrModulePhase(
    ::InteropCallableReferenceLowering,
    name = "InteropCallableReferenceLowering",
    prerequisite = setOf(
        suspendFunctionsLoweringPhase,
        localDeclarationsLoweringPhase,
        localDelegatedPropertiesLoweringPhase,
        callableReferenceLowering
    )
)

private val defaultArgumentStubGeneratorPhase = makeIrModulePhase(
    ::JsDefaultArgumentStubGenerator,
    name = "DefaultArgumentStubGenerator",
)

private val defaultArgumentPatchOverridesPhase = makeIrModulePhase(
    ::DefaultParameterPatchOverridenSymbolsLowering,
    name = "DefaultArgumentsPatchOverrides",
    prerequisite = setOf(defaultArgumentStubGeneratorPhase)
)

private val defaultParameterInjectorPhase = makeIrModulePhase(
    ::JsDefaultParameterInjector,
    name = "DefaultParameterInjector",
    prerequisite = setOf(interopCallableReferenceLoweringPhase, innerClassesLoweringPhase)
)

private fun createDefaultParameterCleanerPhase(context: CommonBackendContext): DefaultParameterCleaner {
    return DefaultParameterCleaner(context)
}

private val defaultParameterCleanerPhase = makeIrModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
)

private val varargLoweringPhase = makeIrModulePhase(
    ::VarargLowering,
    name = "VarargLowering",
    prerequisite = setOf(interopCallableReferenceLoweringPhase)
)

private val propertiesLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    ::PropertiesLowering,
    name = "PropertiesLowering",
)

private val primaryConstructorLoweringPhase = makeIrModulePhase(
    ::PrimaryConstructorLowering,
    name = "PrimaryConstructorLowering",
    prerequisite = setOf(enumClassConstructorLoweringPhase)
)

private val delegateToPrimaryConstructorLoweringPhase = makeIrModulePhase(
    ::DelegateToSyntheticPrimaryConstructor,
    name = "DelegateToSyntheticPrimaryConstructor",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val annotationConstructorLowering = makeIrModulePhase(
    ::AnnotationConstructorLowering,
    name = "AnnotationConstructorLowering",
)

private val initializersLoweringPhase = makeIrModulePhase(
    ::InitializersLowering,
    name = "InitializersLowering",
    prerequisite = setOf(
        enumClassConstructorLoweringPhase, primaryConstructorLoweringPhase, annotationConstructorLowering, localDeclarationExtractionPhase
    )
)

private fun createInitializersCleanupLoweringPhase(context: CommonBackendContext): InitializersCleanupLowering {
    return InitializersCleanupLowering(context)
}

private val initializersCleanupLoweringPhase = makeIrModulePhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    prerequisite = setOf(initializersLoweringPhase)
)

private val externalPropertyOverridingLowering = makeIrModulePhase(
    ::ExternalPropertyOverridingLowering,
    name = "ExternalPropertyOverridingLowering",
    prerequisite = setOf(primaryConstructorLoweringPhase, initializersLoweringPhase)
)

private val multipleCatchesLoweringPhase = makeIrModulePhase(
    ::MultipleCatchesLowering,
    name = "MultipleCatchesLowering",
)

private val bridgesConstructionPhase = makeIrModulePhase(
    ::JsBridgesConstruction,
    name = "BridgesConstruction",
    prerequisite = setOf(suspendFunctionsLoweringPhase)
)

private val singleAbstractMethodPhase = makeIrModulePhase(
    ::JsSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
)

private val typeOperatorLoweringPhase = makeIrModulePhase(
    ::TypeOperatorLowering,
    name = "TypeOperatorLowering",
    prerequisite = setOf(
        bridgesConstructionPhase,
        removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,
        singleAbstractMethodPhase,
        interopCallableReferenceLoweringPhase,
    )
)

private val secondaryConstructorLoweringPhase = makeIrModulePhase(
    ::SecondaryConstructorLowering,
    name = "SecondaryConstructorLoweringPhase",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val secondaryFactoryInjectorLoweringPhase = makeIrModulePhase(
    ::SecondaryFactoryInjectorLowering,
    name = "SecondaryFactoryInjectorLoweringPhase",
    prerequisite = setOf(innerClassesLoweringPhase)
)

private val constLoweringPhase = makeIrModulePhase(
    ::ConstLowering,
    name = "ConstLowering",
)

private fun createInlineClassDeclarationLoweringPhase(context: JsIrBackendContext): InlineClassLowering.InlineClassDeclarationLowering {
    return JsInlineClassLowering(context).inlineClassDeclarationLowering
}

private val inlineClassDeclarationLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    { JsInlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
)

// Const lowering generates inline class constructors for unsigned integers which should be lowered by this lowering
// This annotation must be placed on `inlineClassUsageLowering` but it is hard to achieve because this lowering is inner inside class in the common module
//@PhasePrerequisites(ConstLowering::class)
private fun createInlineClassUsageLoweringPhase(context: JsIrBackendContext): InlineClassLowering.InlineClassUsageLowering {
    return JsInlineClassLowering(context).inlineClassUsageLowering
}

private val inlineClassUsageLoweringPhase = makeIrModulePhase(
    { JsInlineClassLowering(it).inlineClassUsageLowering },
    name = "InlineClassUsageLowering",
    prerequisite = setOf(
        // Const lowering generates inline class constructors for unsigned integers
        // which should be lowered by this lowering
        constLoweringPhase
    )
)

private val expressionBodyTransformer = makeIrModulePhase(
    ::ExpressionBodyTransformer,
    name = "ExpressionBodyTransformer",
)

private fun createAutoboxingTransformerPhase(context: JsCommonBackendContext): AutoboxingTransformer {
    return AutoboxingTransformer(context, replaceTypesInsideInlinedFunctionBlock = true)
}

private val autoboxingTransformerPhase = makeIrModulePhase<JsIrBackendContext>(
    { AutoboxingTransformer(it, replaceTypesInsideInlinedFunctionBlock = true) },
    name = "AutoboxingTransformer",
)

private val blockDecomposerLoweringPhase = makeIrModulePhase(
    ::JsBlockDecomposerLowering,
    name = "BlockDecomposerLowering",
    prerequisite = setOf(typeOperatorLoweringPhase, suspendFunctionsLoweringPhase)
)

private val jsClassUsageInReflectionPhase = makeIrModulePhase(
    ::JsClassUsageInReflectionLowering,
    name = "JsClassUsageInReflectionLowering",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val classReferenceLoweringPhase = makeIrModulePhase(
    ::JsClassReferenceLowering,
    name = "JsClassReferenceLowering",
    prerequisite = setOf(jsClassUsageInReflectionPhase)
)

private val primitiveCompanionLoweringPhase = makeIrModulePhase(
    ::PrimitiveCompanionLowering,
    name = "PrimitiveCompanionLowering",
)

private val callsLoweringPhase = makeIrModulePhase(
    ::CallsLowering,
    name = "CallsLowering",
)

private val staticMembersLoweringPhase = makeIrModulePhase(
    ::StaticMembersLowering,
    name = "StaticMembersLowering",
)

private val objectDeclarationLoweringPhase = makeIrModulePhase(
    ::ObjectDeclarationLowering,
    name = "ObjectDeclarationLowering",
    prerequisite = setOf(enumClassCreateInitializerLoweringPhase)
)

private val invokeStaticInitializersPhase = makeIrModulePhase(
    ::InvokeStaticInitializersLowering,
    name = "IntroduceStaticInitializersLowering",
    prerequisite = setOf(objectDeclarationLoweringPhase)
)

private val es6AddBoxParameterToConstructorsLowering = makeIrModulePhase(
    ::ES6AddBoxParameterToConstructorsLowering,
    name = "ES6AddBoxParameterToConstructorsLowering",
)

private val es6SyntheticPrimaryConstructorLowering = makeIrModulePhase(
    ::ES6SyntheticPrimaryConstructorLowering,
    name = "ES6SyntheticPrimaryConstructorLowering",
    prerequisite = setOf(es6AddBoxParameterToConstructorsLowering)
)

private val es6ConstructorLowering = makeIrModulePhase(
    ::ES6ConstructorLowering,
    name = "ES6ConstructorLowering",
    prerequisite = setOf(es6SyntheticPrimaryConstructorLowering)
)

private val es6ConstructorUsageLowering = makeIrModulePhase(
    ::ES6ConstructorCallLowering,
    name = "ES6ConstructorCallLowering",
    prerequisite = setOf(es6ConstructorLowering)
)

private val objectUsageLoweringPhase = makeIrModulePhase(
    ::ObjectUsageLowering,
    name = "ObjectUsageLowering",
    prerequisite = setOf(primaryConstructorLoweringPhase)
)

private val escapedIdentifiersLowering = makeIrModulePhase(
    ::EscapedIdentifiersLowering,
    name = "EscapedIdentifiersLowering",
)

private val cleanupLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    ::CleanupLowering,
    name = "CleanupLowering",
)

private val jsSuspendArityStorePhase = makeIrModulePhase(
    ::JsSuspendArityStoreLowering,
    name = "JsSuspendArityStoreLowering",
)

private fun createConstEvaluationPhase(context: JsIrBackendContext): ConstEvaluationLowering {
    val configuration = IrInterpreterConfiguration(
        printOnlyExceptionMessage = true,
        platform = JsPlatforms.defaultJsPlatform,
    )
    return ConstEvaluationLowering(context, configuration = configuration)
}

val constEvaluationPhase = makeIrModulePhase<JsIrBackendContext>(
    { context ->
        val configuration = IrInterpreterConfiguration(
            printOnlyExceptionMessage = true,
            platform = JsPlatforms.defaultJsPlatform,
        )
        ConstEvaluationLowering(context, configuration = configuration)
    },
    name = "ConstEvaluationLowering",
)

val mainFunctionCallWrapperLowering = makeIrModulePhase<JsIrBackendContext>(
    ::MainFunctionCallWrapperLowering,
    name = "MainFunctionCallWrapperLowering",
)

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

fun getJsLowerings(): List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> {
    val phases = listOfNotNull<(JsIrBackendContext) -> ModuleLoweringPass>(
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
        ::InitializersLowering,
        ::createInitializersCleanupLoweringPhase,
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
        ::JsSuspendFunctionWithGeneratorsLowering,
        ::JsSuspendFunctionsLowering,
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
        ::DefaultParameterPatchOverridenSymbolsLowering,
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

private val es6CollectConstructorsWhichNeedBoxParameterLowering = makeIrModulePhase(
    ::ES6CollectConstructorsWhichNeedBoxParameters,
    name = "ES6CollectConstructorsWhichNeedBoxParameters",
)

private val es6BoxParameterOptimization = makeIrModulePhase(
    ::ES6ConstructorBoxParameterOptimizationLowering,
    name = "ES6ConstructorBoxParameterOptimizationLowering",
    prerequisite = setOf(es6CollectConstructorsWhichNeedBoxParameterLowering)
)

private val es6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering = makeIrModulePhase(
    ::ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering,
    name = "ES6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering",
)

private val es6PrimaryConstructorOptimizationLowering = makeIrModulePhase(
    ::ES6PrimaryConstructorOptimizationLowering,
    name = "ES6PrimaryConstructorOptimizationLowering",
    prerequisite = setOf(es6CollectPrimaryConstructorsWhichCouldBeOptimizedLowering)
)

private val es6PrimaryConstructorUsageOptimizationLowering = makeIrModulePhase(
    ::ES6PrimaryConstructorUsageOptimizationLowering,
    name = "ES6PrimaryConstructorUsageOptimizationLowering",
    prerequisite = setOf(es6BoxParameterOptimization, es6PrimaryConstructorOptimizationLowering)
)

private val purifyObjectInstanceGetters = makeIrModulePhase(
    ::PurifyObjectInstanceGettersLowering,
    name = "PurifyObjectInstanceGettersLowering",
)

private val inlineObjectsWithPureInitialization = makeIrModulePhase(
    ::InlineObjectsWithPureInitializationLowering,
    name = "InlineObjectsWithPureInitializationLowering",
    prerequisite = setOf(purifyObjectInstanceGetters)
)

val optimizationLoweringList: List<NamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> = run {
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
