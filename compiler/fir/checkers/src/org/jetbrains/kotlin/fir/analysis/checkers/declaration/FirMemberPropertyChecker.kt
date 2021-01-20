/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberPropertyChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (member in declaration.declarations) {
            if (member is FirProperty) {
                checkProperty(declaration, member, context, reporter)
            }
        }
    }

    private fun checkProperty(
        containingDeclaration: FirRegularClass,
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = with(FirModifierList) { property.source.getModifierList() }
        val hasAbstractModifier = modifierList?.modifiers?.any { it.token == KtTokens.ABSTRACT_KEYWORD } == true
        val isAbstract = property.isAbstract || hasAbstractModifier
        if (containingDeclaration.isInterface &&
            Visibilities.isPrivate(property.visibility) &&
            !isAbstract &&
            (property.getter == null || property.getter is FirDefaultPropertyAccessor)
        ) {
            property.source?.let {
                reporter.report(it, FirErrors.PRIVATE_PROPERTY_IN_INTERFACE)
            }
        }

        if (isAbstract) {
            if (!containingDeclaration.canHaveAbstractDeclaration) {
                property.source?.let {
                    reporter.report(FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(it, property, containingDeclaration))
                    return
                }
            }

            if (property.delegate != null) {
                property.delegate!!.source?.let {
                    if (containingDeclaration.isInterface) {
                        reporter.report(it, FirErrors.DELEGATED_PROPERTY_IN_INTERFACE)
                    } else {
                        reporter.report(it, FirErrors.ABSTRACT_DELEGATED_PROPERTY)
                    }
                }
            }

            checkAccessor(property.getter, property.delegate) { src, symbol ->
                reporter.report(src, FirErrors.ABSTRACT_PROPERTY_WITH_GETTER)
            }
            checkAccessor(property.setter, property.delegate) { src, symbol ->
                if (symbol.fir.visibility == Visibilities.Private && property.visibility != Visibilities.Private) {
                    reporter.report(src, FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY)
                } else {
                    reporter.report(src, FirErrors.ABSTRACT_PROPERTY_WITH_SETTER)
                }
            }
        }

        checkPropertyInitializer(containingDeclaration, property, isAbstract, reporter)

        val hasOpenModifier = modifierList?.modifiers?.any { it.token == KtTokens.OPEN_KEYWORD } == true
        if (hasOpenModifier &&
            containingDeclaration.isInterface &&
            !hasAbstractModifier &&
            property.isAbstract &&
            !isInsideExpectClass(containingDeclaration, context)
        ) {
            property.source?.let {
                reporter.report(it, FirErrors.REDUNDANT_OPEN_IN_INTERFACE)
            }
        }
        val isOpen = property.isOpen || hasOpenModifier
        if (isOpen) {
            checkAccessor(property.setter, property.delegate) { src, symbol ->
                if (symbol.fir.visibility == Visibilities.Private && property.visibility != Visibilities.Private) {
                    reporter.report(src, FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY)
                }
            }
        }
    }

    private fun checkPropertyInitializer(
        containingDeclaration: FirRegularClass,
        property: FirProperty,
        propertyIsAbstract: Boolean,
        reporter: DiagnosticReporter
    ) {
        property.initializer?.source?.let {
            if (propertyIsAbstract) {
                reporter.report(it, FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER)
            } else if (containingDeclaration.isInterface) {
                reporter.report(it, FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE)
            }
        }
        if (propertyIsAbstract) {
            if (property.initializer == null && property.delegate == null && property.returnTypeRef is FirImplicitTypeRef) {
                property.source?.let {
                    reporter.report(it, FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER)
                }
            }
        }
    }

    private fun checkAccessor(
        accessor: FirPropertyAccessor?,
        delegate: FirExpression?,
        report: (FirSourceElement, FirPropertyAccessorSymbol) -> Unit,
    ) {
        if (accessor != null && accessor !is FirDefaultPropertyAccessor && accessor.hasBody && delegate == null) {
            accessor.source?.let {
                report.invoke(it, accessor.symbol)
            }
        }
    }

}
