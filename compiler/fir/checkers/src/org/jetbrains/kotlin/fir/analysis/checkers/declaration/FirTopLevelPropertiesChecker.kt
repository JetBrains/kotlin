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
import org.jetbrains.kotlin.fir.analysis.cfa.checkPropertyAccesses
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirTopLevelPropertiesChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        val info = declaration.collectionInitializationInfo(context, reporter)
        for (innerDeclaration in declaration.declarations) {
            if (innerDeclaration is FirProperty) {
                val symbol = innerDeclaration.symbol
                val isDefinitelyAssigned = info?.get(symbol)?.isDefinitelyVisited() == true
                checkProperty(containingDeclaration = null, innerDeclaration, isDefinitelyAssigned, context, reporter, reachable = true)
            }
        }
    }

    private fun FirFile.collectionInitializationInfo(
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ): PropertyInitializationInfo? {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null

        // Scripts are nested as a single declaration under FirFiles and contain their own statements. To properly check all "top-level"
        // properties, script statements need to be unwrapped.
        val topLevelProperties = when (val script = declarations.singleOrNull()) {
            is FirScript -> script.statements.filterIsInstance<FirProperty>()
            else -> declarations.filterIsInstance<FirProperty>()
        }

        val propertySymbols = topLevelProperties.mapNotNullTo(mutableSetOf()) { declaration ->
            (declaration.symbol as? FirPropertySymbol)?.takeIf { it.requiresInitialization(isForInitialization = true) }
        }
        if (propertySymbols.isEmpty()) return null

        // TODO, KT-59803: merge with `FirPropertyInitializationAnalyzer` for fewer passes.
        val data = PropertyInitializationInfoData(propertySymbols, receiver = null, graph)
        data.checkPropertyAccesses(isForInitialization = true, context, reporter)
        return data.getValue(graph.exitNode)[NormalPath]
    }
}

// Matched FE 1.0's [DeclarationsChecker#checkPropertyInitializer].
internal fun checkPropertyInitializer(
    containingClass: FirClass?,
    property: FirProperty,
    modifierList: FirModifierList?,
    isDefinitelyAssigned: Boolean,
    reporter: DiagnosticReporter,
    context: CheckerContext,
    reachable: Boolean = true,
) {
    val inInterface = containingClass?.isInterface == true
    val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
    val isAbstract = property.isAbstract || hasAbstractModifier
    if (isAbstract) {
        val returnTypeRef = property.returnTypeRef
        if (property.initializer == null &&
            property.delegate == null &&
            returnTypeRef is FirErrorTypeRef && returnTypeRef.diagnostic is ConeLocalVariableNoTypeOrInitializer
        ) {
            property.source?.let {
                reporter.reportOn(it, FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER, context)
            }
        }
        return
    }

    val backingFieldRequired = property.hasBackingField
    if (inInterface && backingFieldRequired && property.hasAnyAccessorImplementation) {
        property.source?.let {
            reporter.reportOn(it, FirErrors.BACKING_FIELD_IN_INTERFACE, context)
        }
    }

    val isExpect = property.isEffectivelyExpect(containingClass, context)

    when {
        property.initializer != null -> {
            property.initializer?.source?.let {
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
                    property.receiverParameter != null -> {
                        reporter.reportOn(it, FirErrors.EXTENSION_PROPERTY_WITH_BACKING_FIELD, context)
                    }
                }
            }
        }
        property.delegate != null -> {
            property.delegate?.source?.let {
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
            val propertySource = property.source ?: return
            val isExternal = property.isEffectivelyExternal(containingClass, context)
            val isCorrectlyInitialized =
                property.initializer != null || isDefinitelyAssigned && !property.hasSetterAccessorImplementation &&
                        property.getEffectiveModality(containingClass, context.languageVersionSettings) != Modality.OPEN
            if (
                backingFieldRequired &&
                !inInterface &&
                !property.isLateInit &&
                !isExpect &&
                !isCorrectlyInitialized &&
                !isExternal &&
                !property.hasExplicitBackingField
            ) {
                if (property.receiverParameter != null && !property.hasAllAccessorImplementation) {
                    reporter.reportOn(propertySource, FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, context)
                } else if (reachable) {
                    val isOpenValDeferredInitDeprecationWarning =
                        !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitOpenValDeferredInitialization) &&
                                property.getEffectiveModality(containingClass, context.languageVersionSettings) == Modality.OPEN && property.isVal &&
                                isDefinitelyAssigned
                    // KT-61228
                    val isFalsePositiveDeferredInitDeprecationWarning = isOpenValDeferredInitDeprecationWarning &&
                            property.getEffectiveModality(containingClass) == Modality.FINAL
                    if (!isFalsePositiveDeferredInitDeprecationWarning) {
                        reportMustBeInitialized(
                            property,
                            isDefinitelyAssigned,
                            containingClass,
                            propertySource,
                            isOpenValDeferredInitDeprecationWarning,
                            reporter,
                            context
                        )
                    }
                }
            }
            if (property.isLateInit) {
                if (isExpect) {
                    reporter.reportOn(propertySource, FirErrors.EXPECTED_LATEINIT_PROPERTY, context)
                }
                // TODO, KT-59807: like [BindingContext.MUST_BE_LATEINIT], we should consider variable with uninitialized error.
                if (backingFieldRequired && !inInterface && isCorrectlyInitialized) {
                    if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                        reporter.reportOn(propertySource, FirErrors.UNNECESSARY_LATEINIT, context)
                    }
                }
            }
        }
    }
}

private fun reportMustBeInitialized(
    property: FirProperty,
    isDefinitelyAssigned: Boolean,
    containingClass: FirClass?,
    propertySource: KtSourceElement,
    isOpenValDeferredInitDeprecationWarning: Boolean,
    reporter: DiagnosticReporter,
    context: CheckerContext,
) {
    check(!property.isAbstract) { "${::reportMustBeInitialized.name} isn't called for abstract properties" }
    val suggestMakingItFinal = containingClass != null &&
            !property.hasSetterAccessorImplementation &&
            property.getEffectiveModality(containingClass, context.languageVersionSettings) != Modality.FINAL &&
            isDefinitelyAssigned
    val suggestMakingItAbstract = containingClass != null && !property.hasAnyAccessorImplementation
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

private val FirPropertyAccessor?.hasImplementation: Boolean
    get() = (this !is FirDefaultPropertyAccessor && this?.hasBody == true)
private val FirProperty.hasSetterAccessorImplementation: Boolean
    get() = setter.hasImplementation
private val FirProperty.hasAnyAccessorImplementation: Boolean
    get() = getter.hasImplementation || setter.hasImplementation

private val FirProperty.hasAllAccessorImplementation: Boolean
    get() = getter.hasImplementation && (isVal || setter.hasImplementation)

private fun FirProperty.getEffectiveModality(containingClass: FirClass?): Modality? =
    when (status.modality == Modality.OPEN && containingClass?.status?.modality == Modality.FINAL) {
        true -> Modality.FINAL
        false -> status.modality
    }

private fun FirProperty.getEffectiveModality(containingClass: FirClass?, languageVersionSettings: LanguageVersionSettings): Modality? =
    when (languageVersionSettings.supportsFeature(LanguageFeature.TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck)) {
        true -> getEffectiveModality(containingClass)
        false -> status.modality
    }
