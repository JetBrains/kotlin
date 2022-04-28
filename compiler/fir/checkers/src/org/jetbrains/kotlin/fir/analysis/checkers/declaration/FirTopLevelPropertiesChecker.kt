/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
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

        withSuppressedDiagnostics(declaration, context) { ctx ->
            checkPropertyInitializer(
                containingClass = null,
                declaration,
                modifierList,
                isInitialized = declaration.initializer != null,
                reporter,
                ctx
            )
            checkExpectDeclarationVisibilityAndBody(declaration, source, reporter, ctx)
        }
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
    isInitialized: Boolean,
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
    if (inInterface && backingFieldRequired && property.hasAccessorImplementation) {
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
                    property.receiverTypeRef != null -> {
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
            if (
                backingFieldRequired &&
                !inInterface &&
                !property.isLateInit &&
                !isExpect &&
                !isInitialized &&
                !isExternal &&
                !property.hasExplicitBackingField
            ) {
                if (property.receiverTypeRef != null && !property.hasAccessorImplementation) {
                    reporter.reportOn(propertySource, FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, context)
                } else if (reachable) { // TODO: can be suppressed not to report diagnostics about no body
                    if (containingClass == null || property.hasAccessorImplementation) {
                        reporter.reportOn(propertySource, FirErrors.MUST_BE_INITIALIZED, context)
                    } else {
                        reporter.reportOn(propertySource, FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT, context)
                    }
                }
            }
            if (property.isLateInit) {
                if (isExpect) {
                    reporter.reportOn(propertySource, FirErrors.EXPECTED_LATEINIT_PROPERTY, context)
                }
                // TODO: like [BindingContext.MUST_BE_LATEINIT], we should consider variable with uninitialized error.
                if (backingFieldRequired && !inInterface && isInitialized) {
                    if (context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
                        reporter.reportOn(propertySource, FirErrors.UNNECESSARY_LATEINIT, context)
                    }
                }
            }
        }
    }
}

private val FirProperty.hasAccessorImplementation: Boolean
    get() = (getter !is FirDefaultPropertyAccessor && getter?.hasBody == true) ||
            (setter !is FirDefaultPropertyAccessor && setter?.hasBody == true)
