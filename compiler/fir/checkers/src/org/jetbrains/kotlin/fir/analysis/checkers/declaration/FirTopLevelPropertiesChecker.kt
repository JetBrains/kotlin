/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.PropertyInitializationCheckProcessor
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfo
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.scopes.impl.FirScriptDeclarationsScope
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

// See old FE's [DeclarationsChecker]
object FirTopLevelPropertiesChecker : FirFileChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        @OptIn(DirectDeclarationsAccess::class)
        val topLevelProperties = declaration.declarations.filterIsInstance<FirProperty>()
        checkFileLikeDeclaration(declaration, topLevelProperties.map { it.symbol }, context, reporter)
    }
}

object FirScriptPropertiesChecker : FirScriptChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirScript, context: CheckerContext, reporter: DiagnosticReporter) {
        val topLevelPropertySymbols = mutableListOf<FirPropertySymbol>()
        FirScriptDeclarationsScope(context.session, declaration).processAllCallables { callable ->
            if (callable is FirPropertySymbol) {
                topLevelPropertySymbols += callable
            }
        }
        checkFileLikeDeclaration(declaration, topLevelPropertySymbols, context, reporter)
    }
}

private fun checkFileLikeDeclaration(
    declaration: FirDeclaration,
    topLevelPropertySymbols: List<FirPropertySymbol>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    val info = declaration.collectionInitializationInfo(topLevelPropertySymbols, context, reporter)
    for (topLevelPropertySymbol in topLevelPropertySymbols) {
        val isDefinitelyAssigned = info?.get(topLevelPropertySymbol)?.isDefinitelyVisited() == true
        checkProperty(containingDeclaration = null, topLevelPropertySymbol, isDefinitelyAssigned, context, reporter, reachable = true)
    }
}

private fun FirDeclaration.collectionInitializationInfo(
    topLevelPropertySymbols: List<FirPropertySymbol>,
    context: CheckerContext,
    reporter: DiagnosticReporter,
): VariableInitializationInfo? {
    val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null

    val propertySymbols = topLevelPropertySymbols.mapNotNullTo(mutableSetOf()) { propertySymbol ->
        propertySymbol.takeIf { it.requiresInitialization(isForInitialization = true) }
    }
    if (propertySymbols.isEmpty()) return null

    // TODO, KT-59803: merge with `FirPropertyInitializationAnalyzer` for fewer passes.
    val data = PropertyInitializationInfoData(propertySymbols, conditionallyInitializedProperties = emptySet(), receiver = null, graph)
    PropertyInitializationCheckProcessor.check(data, isForInitialization = true, context, reporter)
    return data.getValue(graph.exitNode)[NormalPath]
}

// Matched FE 1.0's [DeclarationsChecker#checkPropertyInitializer].
internal fun checkPropertyInitializer(
    containingClass: FirClass?,
    propertySymbol: FirPropertySymbol,
    modifierList: FirModifierList?,
    isDefinitelyAssigned: Boolean,
    reporter: DiagnosticReporter,
    context: CheckerContext,
    reachable: Boolean = true,
) {
    val inInterface = containingClass?.isInterface == true
    val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
    val isAbstract = propertySymbol.isAbstract || hasAbstractModifier
    if (isAbstract) {
        val returnTypeRef = propertySymbol.resolvedReturnTypeRef
        if (!propertySymbol.hasInitializer &&
            propertySymbol.delegate == null &&
            returnTypeRef.noExplicitType()
        ) {
            propertySymbol.source?.let {
                reporter.reportOn(it, FirErrors.ABSTRACT_PROPERTY_WITHOUT_TYPE, context)
            }
        }
        return
    }

    val backingFieldRequired = propertySymbol.hasBackingField
    if (inInterface && backingFieldRequired && propertySymbol.hasAnyAccessorImplementation) {
        propertySymbol.source?.let {
            reporter.reportOn(it, FirErrors.BACKING_FIELD_IN_INTERFACE, context)
        }
    }

    val isExpect = propertySymbol.isEffectivelyExpect(containingClass, context)

    when {
        propertySymbol.hasInitializer -> {
            propertySymbol.initializerSource?.let {
                when {
                    inInterface -> {
                        reporter.reportOn(it, FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE, context)
                    }
                    isExpect -> {
                        reporter.reportOn(it, FirErrors.EXPECTED_PROPERTY_INITIALIZER, context)
                    }
                    !backingFieldRequired -> {
                        reporter.reportOn(it, FirErrors.PROPERTY_INITIALIZER_NO_BACKING_FIELD, context)
                    }
                    propertySymbol.receiverParameterSymbol != null -> {
                        reporter.reportOn(it, FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD, context)
                    }
                }
            }
        }
        propertySymbol.delegate != null -> {
            propertySymbol.delegate?.source?.let {
                when {
                    inInterface -> {
                        reporter.reportOn(it, FirErrors.DELEGATED_PROPERTY_IN_INTERFACE, context)
                    }
                    isExpect -> {
                        reporter.reportOn(it, FirErrors.EXPECTED_DELEGATED_PROPERTY, context)
                    }
                }
            }
        }
        else -> {
            val propertySource = propertySymbol.source ?: return
            val isExternal = propertySymbol.isEffectivelyExternal(containingClass, context)
            val noExplicitType =
                propertySymbol.resolvedReturnTypeRef.noExplicitType() &&
                        !propertySymbol.hasExplicitBackingField &&
                        (propertySymbol.getterSymbol?.isDefault == true || (propertySymbol.getterSymbol?.hasBody == true && propertySymbol.getterSymbol?.resolvedReturnTypeRef?.noExplicitType() == true))
            val isCorrectlyInitialized =
                propertySymbol.hasInitializer || isDefinitelyAssigned && !propertySymbol.hasSetterAccessorImplementation &&
                        (propertySymbol.getEffectiveModality(containingClass, context.languageVersionSettings) != Modality.OPEN ||
                                // Drop this workaround after KT-64980 is fixed
                                propertySymbol.effectiveVisibility == org.jetbrains.kotlin.descriptors.EffectiveVisibility.PrivateInClass)

            var initializationError = false
            if (
                backingFieldRequired &&
                !inInterface &&
                !propertySymbol.isLateInit &&
                !isExpect &&
                !isExternal &&
                !propertySymbol.hasExplicitBackingField
            ) {
                if (propertySymbol.receiverParameterSymbol != null && !propertySymbol.hasAllAccessorImplementation) {
                    reporter.reportOn(propertySource, FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, context)
                    initializationError = true
                } else if (!isCorrectlyInitialized && reachable) {
                    val isOpenValDeferredInitDeprecationWarning =
                        !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitOpenValDeferredInitialization) &&
                                propertySymbol.getEffectiveModality(containingClass, context.languageVersionSettings) == Modality.OPEN &&
                                propertySymbol.isVal &&
                                isDefinitelyAssigned
                    // KT-61228
                    val isFalsePositiveDeferredInitDeprecationWarning = isOpenValDeferredInitDeprecationWarning &&
                            propertySymbol.getEffectiveModality(containingClass) == Modality.FINAL
                    if (!isFalsePositiveDeferredInitDeprecationWarning) {
                        reportMustBeInitialized(
                            propertySymbol,
                            isDefinitelyAssigned,
                            containingClass,
                            propertySource,
                            isOpenValDeferredInitDeprecationWarning,
                            reporter,
                            context
                        )
                        initializationError = true
                    }
                }
            }

            if (!initializationError && noExplicitType) {
                reporter.reportOn(
                    propertySource,
                    if (propertySymbol.isLateInit) FirErrors.LATEINIT_PROPERTY_WITHOUT_TYPE else FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER,
                    context
                )
            }

            if (propertySymbol.isLateInit) {
                if (isExpect) {
                    reporter.reportOn(propertySource, FirErrors.EXPECTED_LATEINIT_PROPERTY, context)
                }
                // TODO, KT-59807: like [BindingContext.MUST_BE_LATEINIT], we should consider variable with uninitialized error.
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                    if (
                        backingFieldRequired &&
                        !inInterface &&
                        isCorrectlyInitialized &&
                        propertySymbol.backingFieldSymbol?.hasAnnotation(StandardClassIds.Annotations.Transient, context.session) != true &&
                        !propertySymbol.hasAnnotation(KOTLINX_SERIALIZATION_TRANSIENT, context.session)
                    ) {
                        reporter.reportOn(propertySource, FirErrors.UNNECESSARY_LATEINIT, context)
                    }
                }
            }
        }
    }
}

private fun FirTypeRef.noExplicitType(): Boolean {
    return this is FirErrorTypeRef && diagnostic is ConeLocalVariableNoTypeOrInitializer
}

private fun reportMustBeInitialized(
    propertySymbol: FirPropertySymbol,
    isDefinitelyAssigned: Boolean,
    containingClass: FirClass?,
    propertySource: KtSourceElement,
    isOpenValDeferredInitDeprecationWarning: Boolean,
    reporter: DiagnosticReporter,
    context: CheckerContext,
) {
    check(!propertySymbol.isAbstract) { "${::reportMustBeInitialized.name} isn't called for abstract properties" }
    val suggestMakingItFinal = containingClass != null &&
            !propertySymbol.hasSetterAccessorImplementation &&
            propertySymbol.getEffectiveModality(containingClass, context.languageVersionSettings) != Modality.FINAL &&
            isDefinitelyAssigned
    val suggestMakingItAbstract = containingClass != null && !propertySymbol.hasAnyAccessorImplementation
    if (isOpenValDeferredInitDeprecationWarning && !suggestMakingItFinal && suggestMakingItAbstract) {
        error("Not reachable case. Every \"open val + deferred init\" case that could be made `abstract`, also could be made `final`")
    }
    val isMissedMustBeInitializedDeprecationWarning =
        !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor) &&
                containingClass != null &&
                containingClass.primaryConstructorIfAny(context.session) == null &&
                isDefinitelyAssigned
    val factory = when {
        suggestMakingItFinal && suggestMakingItAbstract -> FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT
        suggestMakingItFinal -> FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL
        suggestMakingItAbstract -> FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT
        else -> FirErrors.MUST_BE_INITIALIZED
    }
    reporter.reportOn(
        propertySource,
        when (isMissedMustBeInitializedDeprecationWarning || isOpenValDeferredInitDeprecationWarning) {
            true -> factory.deprecationWarning
            false -> factory
        },
        context
    )
}

private val KtDiagnosticFactory0.deprecationWarning
    get() = when (this) {
        FirErrors.MUST_BE_INITIALIZED -> FirErrors.MUST_BE_INITIALIZED_WARNING
        FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT -> FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING
        FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL -> FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING
        FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT -> FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING
        else -> error("Only MUST_BE_INITIALIZED is supported")
    }

private val FirPropertyAccessorSymbol?.hasImplementation: Boolean
    get() = (this?.isDefault != true && this?.hasBody == true)
private val FirPropertySymbol.hasSetterAccessorImplementation: Boolean
    get() = setterSymbol.hasImplementation
private val FirPropertySymbol.hasAnyAccessorImplementation: Boolean
    get() = getterSymbol.hasImplementation || setterSymbol.hasImplementation

private val FirPropertySymbol.hasAllAccessorImplementation: Boolean
    get() = getterSymbol.hasImplementation && (isVal || setterSymbol.hasImplementation)

private fun FirPropertySymbol.getEffectiveModality(containingClass: FirClass?): Modality? =
    when (resolvedStatus.modality == Modality.OPEN && containingClass?.status?.modality == Modality.FINAL) {
        true -> Modality.FINAL
        false -> resolvedStatus.modality
    }

private fun FirPropertySymbol.getEffectiveModality(
    containingClass: FirClass?,
    languageVersionSettings: LanguageVersionSettings
): Modality? =
    when (languageVersionSettings.supportsFeature(LanguageFeature.TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck)) {
        true -> getEffectiveModality(containingClass)
        false -> resolvedStatus.modality
    }

private val KOTLINX_SERIALIZATION_TRANSIENT = ClassId(
    FqName.fromSegments("kotlinx.serialization".split(".")),
    Name.identifier("Transient")
)
