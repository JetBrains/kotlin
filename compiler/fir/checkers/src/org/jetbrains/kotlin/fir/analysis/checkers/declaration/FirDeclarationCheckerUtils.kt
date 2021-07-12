/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.FirModifierList
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.modality
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.containingClassAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.lexer.KtTokens

internal fun isInsideExpectClass(containingClass: FirClass, context: CheckerContext): Boolean {
    return isInsideSpecificClass(containingClass, context) { klass -> klass is FirRegularClass && klass.isExpect }
}

internal fun isInsideExternalClass(containingClass: FirClass, context: CheckerContext): Boolean {
    return isInsideSpecificClass(containingClass, context) { klass -> klass is FirRegularClass && klass.isExternal }
}

// Note that the class that contains the currently visiting declaration will *not* be in the context's containing declarations *yet*.
private inline fun isInsideSpecificClass(
    containingClass: FirClass,
    context: CheckerContext,
    predicate: (FirClass) -> Boolean
): Boolean {
    return predicate.invoke(containingClass) ||
            context.containingDeclarations.asReversed().any { it is FirRegularClass && predicate.invoke(it) }
}

internal fun FirMemberDeclaration.isEffectivelyExpect(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean {
    if (this.isExpect) return true

    return containingClass != null && isInsideExpectClass(containingClass, context)
}

internal fun FirMemberDeclaration.isEffectivelyExternal(
    containingClass: FirClass?,
    context: CheckerContext,
): Boolean {
    if (this.isExternal) return true

    if (this is FirPropertyAccessor) {
        // Check containing property
        val property = context.containingDeclarations.last() as FirProperty
        return property.isEffectivelyExternal(containingClass, context)
    }

    if (this is FirProperty) {
        // Property is effectively external if all accessors are external
        if (getter?.isExternal == true && (!isVar || setter?.isExternal == true)) {
            return true
        }
    }

    return containingClass != null && isInsideExternalClass(containingClass, context)
}

// TODO: check class too
internal fun checkExpectDeclarationVisibilityAndBody(
    declaration: FirMemberDeclaration,
    source: FirSourceElement,
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
            if (backingFieldRequired && !inInterface && !property.isLateInit && !isExpect && !isInitialized && !isExternal) {
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
                    reporter.reportOn(propertySource, FirErrors.UNNECESSARY_LATEINIT, context)
                }
            }
        }
    }
}

private val FirProperty.hasAccessorImplementation: Boolean
    get() = (getter !is FirDefaultPropertyAccessor && getter?.hasBody == true) ||
            (setter !is FirDefaultPropertyAccessor && setter?.hasBody == true)

internal val FirClass.canHaveOpenMembers: Boolean get() = modality() != Modality.FINAL || classKind == ClassKind.ENUM_CLASS

internal fun FirRegularClass.isInlineOrValueClass(): Boolean {
    if (this.classKind != ClassKind.CLASS) return false

    return isInline || hasModifier(KtTokens.VALUE_KEYWORD)
}

internal val FirDeclaration.isEnumEntryInitializer: Boolean
    get() {
        if (this !is FirConstructor || !this.isPrimary) return false
        return (containingClassAttr as? ConeClassLookupTagWithFixedSymbol)?.symbol?.fir?.classKind == ClassKind.ENUM_ENTRY
    }

// contract: returns(true) implies (this is FirMemberDeclaration<*>)
internal val FirDeclaration.isLocalMember: Boolean
    get() = when (this) {
        is FirProperty -> this.isLocal
        is FirRegularClass -> this.isLocal
        is FirSimpleFunction -> this.isLocal
        else -> false
    }

internal val FirCallableDeclaration.isExtensionMember: Boolean
    get() {
        return receiverTypeRef != null && dispatchReceiverType != null
    }
