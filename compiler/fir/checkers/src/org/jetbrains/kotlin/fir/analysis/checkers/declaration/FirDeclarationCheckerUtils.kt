/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.lexer.KtTokens

// Note that the class that contains the currently visiting declaration will *not* be in the context's containing declarations *yet*.
internal fun isInsideExpectClass(containingDeclaration: FirRegularClass, context: CheckerContext): Boolean =
    containingDeclaration.isExpect || context.containingDeclarations.asReversed().any { it is FirRegularClass && it.isExpect }

// TODO: check class too
internal fun checkExpectDeclarationVisibilityAndBody(
    declaration: FirMemberDeclaration,
    source: FirSourceElement,
    modifierList: FirModifierList?,
    reporter: DiagnosticReporter,
    context: CheckerContext
) {
    if (declaration.isExpect || modifierList?.modifiers?.any { it.token == KtTokens.EXPECT_KEYWORD } == true) {
        if (Visibilities.isPrivate(declaration.visibility)) {
            reporter.reportOn(source, FirErrors.EXPECTED_PRIVATE_DECLARATION, context)
        }
        if (declaration is FirSimpleFunction && declaration.hasBody) {
            reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
        }
    }
}

internal fun checkPropertyInitializer(
    containingClass: FirRegularClass?,
    property: FirProperty,
    reporter: DiagnosticReporter,
    context: CheckerContext
) {
    val inInterface = containingClass?.isInterface == true
    // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
    // So, our source of truth should be the full modifier list retrieved from the source.
    val modifierList = with(FirModifierList) { property.source.getModifierList() }
    val hasAbstractModifier = modifierList?.modifiers?.any { it.token == KtTokens.ABSTRACT_KEYWORD } == true
    val isAbstract = property.isAbstract || hasAbstractModifier
    if (isAbstract) {
        if (property.initializer == null && property.delegate == null && property.returnTypeRef is FirImplicitTypeRef) {
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

    val isExpect = property.isExpect || modifierList?.modifiers?.any { it.token == KtTokens.EXPECT_KEYWORD } == true

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
            val isExternal = property.isExternal || modifierList?.modifiers?.any { it.token == KtTokens.EXTERNAL_KEYWORD } == true
            // TODO: need to analyze class anonymous initializer to see if the property is initialized there.
            val isUninitialized = false
            if (backingFieldRequired && !inInterface && !property.isLateInit && !isExpect && isUninitialized && !isExternal) {
                property.source?.let {
                    if (property.receiverTypeRef != null && !property.hasAccessorImplementation) {
                        // reporter.reportOn(it, FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT, context)
                    } else {
                        if (containingClass != null || property.hasAccessorImplementation) {
                            // reporter.reportOn(it, FirErrors.MUST_BE_INITIALIZED, context)
                        } else {
                            // reporter.reportOn(it, FirErrors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT, context)
                        }
                    }
                }
            }
        }
    }
}

private val FirProperty.hasAccessorImplementation: Boolean
    get() = (getter !is FirDefaultPropertyAccessor && getter?.hasBody == true) ||
            (setter !is FirDefaultPropertyAccessor && setter?.hasBody == true)


internal val FirClass<*>.canHaveOpenMembers: Boolean get() = modality() != Modality.FINAL || classKind == ClassKind.ENUM_CLASS
