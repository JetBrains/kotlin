/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirTopLevelPropertiesChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        // Only report on top level callable declarations
        if (context.containingDeclarations.size > 1) return

        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = source.getModifierList()

        checkPropertyInitializer(
            containingClass = null,
            declaration,
            modifierList,
            isDeferredInitialized = false, // Only member properties can be deferredly initialized in `init` block
            reporter,
            context
        )
        checkExpectDeclarationVisibilityAndBody(declaration, source, reporter, context)
    }
}

// TODO: check class too
internal fun checkExpectDeclarationVisibilityAndBody(
    declaration: FirMemberDeclaration,
    source: KtSourceElement,
    reporter: DiagnosticReporter,
    context: CheckerContext
) {
    if (declaration.isExpect) {
        if (Visibilities.isPrivate(declaration.visibility)) {
            reporter.reportOn(source, FirErrors.EXPECTED_PRIVATE_DECLARATION, context)
        }
        if (declaration is FirSimpleFunction && declaration.hasBody) {
            reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
        }
    }
}

// Matched FE 1.0's [DeclarationsChecker#checkPropertyInitializer].
internal fun checkPropertyInitializer(
    containingClass: FirClass?,
    property: FirProperty,
    modifierList: FirModifierList?,
    isDeferredInitialized: Boolean,
    reporter: DiagnosticReporter,
    context: CheckerContext,
    reachable: Boolean = true
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
                property.initializer != null || isDeferredInitialized && !property.hasSetterAccessorImplementation &&
                        property.getEffectiveModality(containingClass) != Modality.OPEN
            if (
                backingFieldRequired &&
                !inInterface &&
                !property.isLateInit &&
                !isExpect &&
                !isCorrectlyInitialized &&
                !isExternal &&
                !property.hasExplicitBackingField
            ) {
                if (property.receiverParameter != null && !property.hasAnyAccessorImplementation) {
                    reporter.reportOn(propertySource, FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, context)
                } else if (reachable) { // TODO: can be suppressed not to report diagnostics about no body
                    reportMustBeInitialized(property, isDeferredInitialized, containingClass, propertySource, reporter, context)
                }
            }
            if (property.isLateInit) {
                if (isExpect) {
                    reporter.reportOn(propertySource, FirErrors.EXPECTED_LATEINIT_PROPERTY, context)
                }
                // TODO: like [BindingContext.MUST_BE_LATEINIT], we should consider variable with uninitialized error.
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
    isDeferredInitialized: Boolean,
    containingClass: FirClass?,
    propertySource: KtSourceElement,
    reporter: DiagnosticReporter,
    context: CheckerContext,
) {
    check(!property.isAbstract) { "${::reportMustBeInitialized.name} isn't called for abstract properties" }
    val suggestMakingItFinal = containingClass != null &&
            !property.hasSetterAccessorImplementation &&
            property.getEffectiveModality(containingClass) != Modality.FINAL &&
            isDeferredInitialized
    val suggestMakingItAbstract = containingClass != null && !property.hasAnyAccessorImplementation
    val isOpenValDeferredInitDeprecationWarning =
        !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitOpenValDeferredInitialization) &&
                property.getEffectiveModality(containingClass) == Modality.OPEN && property.isVal &&
                isDeferredInitialized

    val factory = when {
        suggestMakingItFinal && suggestMakingItAbstract -> when (isOpenValDeferredInitDeprecationWarning) {
            true -> FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING
            false -> FirErrors.MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT
        }
        suggestMakingItFinal -> when (isOpenValDeferredInitDeprecationWarning) {
            true -> FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING
            false -> FirErrors.MUST_BE_INITIALIZED_OR_BE_FINAL
        }
        suggestMakingItAbstract -> when (isOpenValDeferredInitDeprecationWarning) {
            true -> error("Not reachable case. Every \"open val + deferred init\" case that could be made `abstract`, also could be made `final`")
            false -> FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT
        }
        else -> when (isOpenValDeferredInitDeprecationWarning) {
            true -> error("Not reachable case. We can always suggest making `open val` property `final`")
            false -> FirErrors.MUST_BE_INITIALIZED
        }
    }
    reporter.reportOn(propertySource, factory, context)
}

private val FirProperty.hasSetterAccessorImplementation: Boolean
    get() = (setter !is FirDefaultPropertyAccessor && setter?.hasBody == true)
private val FirProperty.hasAnyAccessorImplementation: Boolean
    get() = (getter !is FirDefaultPropertyAccessor && getter?.hasBody == true) || hasSetterAccessorImplementation

private fun FirProperty.getEffectiveModality(containingClass: FirClass?): Modality? =
    when (status.modality == Modality.OPEN && containingClass?.status?.modality == Modality.FINAL) {
        true -> Modality.FINAL
        false -> status.modality
    }
