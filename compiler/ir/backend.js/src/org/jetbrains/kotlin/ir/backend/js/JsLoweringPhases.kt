/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.Symbols.Companion.isTypeOfIntrinsic
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.*
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.inline.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.platform.js.JsPlatforms

private fun List<CompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>>.toCompilerPhase() =
    reduce { acc, lowering -> acc.then(lowering) }

private val validateIrBeforeLowering = makeIrModulePhase<JsIrBackendContext>(
    ::IrValidationBeforeLoweringPhase,
    name = "ValidateIrBeforeLowering",
)

private val validateIrAfterInliningOnlyPrivateFunctions = makeIrModulePhase(
    { context: JsIrBackendContext ->
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

private val dumpSyntheticAccessorsPhase = makeIrModulePhase<JsIrBackendContext>(
    ::DumpSyntheticAccessors,
    name = "DumpSyntheticAccessorsPhase",
)

private val validateIrAfterInliningAllFunctions = makeIrModulePhase(
    { context: JsIrBackendContext ->
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

private val validateIrAfterLowering = makeIrModulePhase<JsIrBackendContext>(
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

private val preventExportOfSyntheticDeclarationsLowering = makeIrModulePhase(
    ::ExcludeSyntheticDeclarationsFromExportLowering,
    name = "ExcludeSyntheticDeclarationsFromExportLowering",
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

private val annotationInstantiationLowering = makeIrModulePhase(
    ::JsAnnotationImplementationTransformer,
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

private val kotlinNothingValueExceptionPhase = makeIrModulePhase(
    ::KotlinNothingValueExceptionLowering,
    name = "KotlinNothingValueException",
)

private val stripTypeAliasDeclarationsPhase = makeIrModulePhase<JsIrBackendContext>(
    { StripTypeAliasDeclarationsLowering() },
    name = "StripTypeAliasDeclarations",
)

private val jsCodeOutliningPhase = makeIrModulePhase(
    { context: JsIrBackendContext -> JsCodeOutliningLowering(context, context.intrinsics, context.dynamicType) },
    name = "JsCodeOutliningLowering",
)

private val inlineCallableReferenceToLambdaPhase = makeIrModulePhase<JsIrBackendContext>(
    ::JsInlineCallableReferenceToLambdaPhase,
    name = "JsInlineCallableReferenceToLambdaPhase",
)

private val arrayConstructorPhase = makeIrModulePhase(
    ::ArrayConstructorLowering,
    name = "ArrayConstructor",
    prerequisite = setOf(inlineCallableReferenceToLambdaPhase)
)

private val sharedVariablesLoweringPhase = makeIrModulePhase(
    ::SharedVariablesLowering,
    name = "SharedVariablesLowering",
    prerequisite = setOf(lateinitPhase)
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = makeIrModulePhase(
    ::OuterThisInInlineFunctionsSpecialAccessorLowering,
    name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
)

private val localClassesInInlineLambdasPhase = makeIrModulePhase(
    ::LocalClassesInInlineLambdasLowering,
    name = "LocalClassesInInlineLambdasPhase",
)

private val localClassesInInlineFunctionsPhase = makeIrModulePhase(
    ::LocalClassesInInlineFunctionsLowering,
    name = "LocalClassesInInlineFunctionsPhase",
)

private val localClassesExtractionFromInlineFunctionsPhase = makeIrModulePhase(
    { context -> LocalClassesExtractionFromInlineFunctionsLowering(context) },
    name = "localClassesExtractionFromInlineFunctionsPhase",
    prerequisite = setOf(localClassesInInlineFunctionsPhase)
)

private val legacySyntheticAccessorLoweringPhase = makeIrModulePhase(
    ::LegacySyntheticAccessorLowering,
    name = "LegacySyntheticAccessorLowering",
)

private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = makeIrModulePhase(
    ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
    name = "WrapInlineDeclarationsWithReifiedTypeParametersLowering",
)

private val replaceSuspendIntrinsicLowering = makeIrModulePhase(
    ::ReplaceSuspendIntrinsicLowering,
    name = "ReplaceSuspendIntrinsicLowering",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = makeIrModulePhase(
    { context: JsIrBackendContext ->
        FunctionInlining(
            context,
            JsInlineFunctionResolver(context, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
            produceOuterThisFields = false,
        )
    },
    name = "InlineOnlyPrivateFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

internal val syntheticAccessorGenerationPhase = makeIrModulePhase(
    lowering = ::SyntheticAccessorLowering,
    name = "SyntheticAccessorGeneration",
    prerequisite = setOf(inlineOnlyPrivateFunctionsPhase),
)

/**
 * The second phase of inlining (inline all functions).
 */
private val inlineAllFunctionsPhase = makeIrModulePhase(
    { context: JsIrBackendContext ->
        FunctionInlining(
            context,
            JsInlineFunctionResolver(context, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
            produceOuterThisFields = false,
        )
    },
    name = "InlineAllFunctions",
    prerequisite = setOf(outerThisSpecialAccessorInInlineFunctionsPhase)
)

private val copyInlineFunctionBodyLoweringPhase = makeIrModulePhase(
    ::CopyInlineFunctionBodyLowering,
    name = "CopyInlineFunctionBody",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase = makeIrModulePhase(
    { RemoveInlineDeclarationsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    prerequisite = setOf(inlineAllFunctionsPhase)
)

private val captureStackTraceInThrowablesPhase = makeIrModulePhase(
    ::CaptureStackTraceInThrowables,
    name = "CaptureStackTraceInThrowables",
)

private val throwableSuccessorsLoweringPhase = makeIrModulePhase(
    { context ->
        context.run {
            val extendThrowableSymbol =
                if (es6mode) setPropertiesToThrowableInstanceSymbol else extendThrowableSymbol

            ThrowableLowering(this, extendThrowableSymbol)
        }
    },
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

private val callableReferenceLowering = makeIrModulePhase(
    ::CallableReferenceLowering,
    name = "CallableReferenceLowering",
    prerequisite = setOf(inlineAllFunctionsPhase, wrapInlineDeclarationsWithReifiedTypeParametersLowering)
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
    { LocalDelegatedPropertiesLowering() },
    name = "LocalDelegatedPropertiesLowering",
)

private val localDeclarationsLoweringPhase = makeIrModulePhase(
    { context -> LocalDeclarationsLowering(context, suggestUniqueNames = false) },
    name = "LocalDeclarationsLowering",
    prerequisite = setOf(sharedVariablesLoweringPhase, localDelegatedPropertiesLoweringPhase)
)

private val localClassExtractionPhase = makeIrModulePhase(
    { context -> LocalClassPopupLowering(context) },
    name = "LocalClassExtractionPhase",
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

private val privateMembersLoweringPhase = makeIrModulePhase(
    ::PrivateMembersLowering,
    name = "PrivateMembersLowering",
)

private val privateMemberUsagesLoweringPhase = makeIrModulePhase(
    ::PrivateMemberBodiesLowering,
    name = "PrivateMemberUsagesLowering",
)

private val propertyReferenceLoweringPhase = makeIrModulePhase(
    ::PropertyReferenceLowering,
    name = "PropertyReferenceLowering",
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
    { PropertiesLowering() },
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
        enumClassConstructorLoweringPhase, primaryConstructorLoweringPhase, annotationConstructorLowering, localClassExtractionPhase
    )
)

private val initializersCleanupLoweringPhase = makeIrModulePhase(
    ::InitializersCleanupLowering,
    name = "InitializersCleanupLowering",
    prerequisite = setOf(initializersLoweringPhase)
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
private val inlineClassDeclarationLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    { InlineClassLowering(it).inlineClassDeclarationLowering },
    name = "InlineClassDeclarationLowering",
)

private val inlineClassUsageLoweringPhase = makeIrModulePhase(
    { InlineClassLowering(it).inlineClassUsageLowering },
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

private val implicitlyExportedDeclarationsMarkingLowering = makeIrModulePhase(
    ::ImplicitlyExportedDeclarationsMarkingLowering,
    name = "ImplicitlyExportedDeclarationsMarkingLowering",
)

private val cleanupLoweringPhase = makeIrModulePhase<JsIrBackendContext>(
    { CleanupLowering() },
    name = "CleanupLowering",
)

private val jsSuspendArityStorePhase = makeIrModulePhase(
    ::JsSuspendArityStoreLowering,
    name = "JsSuspendArityStoreLowering",
)

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

fun getJsLowerings(
    configuration: CompilerConfiguration
): List<SimpleNamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>> = listOfNotNull(
    // BEGIN: Common Native/JS/Wasm prefix.
    validateIrBeforeLowering,
    jsCodeOutliningPhase,
    lateinitPhase,
    sharedVariablesLoweringPhase,
    outerThisSpecialAccessorInInlineFunctionsPhase,
    localClassesInInlineLambdasPhase,
    localClassesInInlineFunctionsPhase.takeIf { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    localClassesExtractionFromInlineFunctionsPhase.takeIf { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    inlineCallableReferenceToLambdaPhase,
    arrayConstructorPhase,
    legacySyntheticAccessorLoweringPhase.takeIf { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    wrapInlineDeclarationsWithReifiedTypeParametersLowering,
    inlineOnlyPrivateFunctionsPhase.takeUnless { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    syntheticAccessorGenerationPhase.takeUnless { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    // Note: The validation goes after both `inlineOnlyPrivateFunctionsPhase` and `syntheticAccessorGenerationPhase`
    // just because it goes so in Native.
    validateIrAfterInliningOnlyPrivateFunctions.takeUnless { configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) },
    inlineAllFunctionsPhase,
    dumpSyntheticAccessorsPhase.takeIf {
        !configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING) &&
                configuration[KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR] != null
    },
    validateIrAfterInliningAllFunctions,
    // END: Common Native/JS/Wasm prefix.

    constEvaluationPhase,
    copyInlineFunctionBodyLoweringPhase,
    removeInlineDeclarationsWithReifiedTypeParametersLoweringPhase,
    replaceSuspendIntrinsicLowering,
    prepareCollectionsToExportLowering,
    preventExportOfSyntheticDeclarationsLowering,
    jsStaticLowering,
    inventNamesForLocalClassesPhase,
    collectClassIdentifiersLowering,
    annotationInstantiationLowering,
    expectDeclarationsRemovingPhase,
    stripTypeAliasDeclarationsPhase,
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
    bridgesConstructionPhase,
    typeOperatorLoweringPhase,
    secondaryConstructorLoweringPhase,
    secondaryFactoryInjectorLoweringPhase,
    classReferenceLoweringPhase,
    constLoweringPhase,
    inlineClassDeclarationLoweringPhase,
    inlineClassUsageLoweringPhase,
    expressionBodyTransformer,
    autoboxingTransformerPhase,
    objectDeclarationLoweringPhase,
    blockDecomposerLoweringPhase,
    invokeStaticInitializersPhase,
    objectUsageLoweringPhase,
    es6AddBoxParameterToConstructorsLowering,
    es6SyntheticPrimaryConstructorLowering,
    es6ConstructorLowering,
    es6ConstructorUsageLowering,
    callsLoweringPhase,
    escapedIdentifiersLowering,
    implicitlyExportedDeclarationsMarkingLowering,
    removeImplicitExportsFromCollections,
    mainFunctionCallWrapperLowering,
    cleanupLoweringPhase,
    validateIrAfterLowering,
)

fun getJsPhases(
    configuration: CompilerConfiguration
): SameTypeNamedCompilerPhase<JsIrBackendContext, IrModuleFragment> = SameTypeNamedCompilerPhase(
    name = "IrModuleLowering",
    lower = getJsLowerings(configuration).toCompilerPhase(),
    actions = DEFAULT_IR_ACTIONS,
    nlevels = 1
)

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

val optimizationLoweringList = listOf<SimpleNamedCompilerPhase<JsIrBackendContext, IrModuleFragment, IrModuleFragment>>(
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
    lower = optimizationLoweringList.toCompilerPhase(),
    actions = DEFAULT_IR_ACTIONS,
    nlevels = 1
)
