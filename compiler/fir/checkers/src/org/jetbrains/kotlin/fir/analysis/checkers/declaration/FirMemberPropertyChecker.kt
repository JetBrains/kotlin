/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
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

// See old FE's [DeclarationsChecker]
object FirMemberPropertyChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) {
            return
        }
        for (member in declaration.declarations) {
            if (member is FirProperty) {
                checkProperty(declaration, member, reporter)
            }
        }
    }

    private fun checkProperty(containingDeclaration: FirRegularClass, property: FirProperty, reporter: DiagnosticReporter) {
        if (inInterface(containingDeclaration) &&
            property.visibility == Visibilities.Private &&
            !property.isAbstract &&
            (property.getter == null || property.getter is FirDefaultPropertyAccessor)
        ) {
            property.source?.let { source ->
                reporter.report(source, FirErrors.PRIVATE_PROPERTY_IN_INTERFACE)
            }
        }

        if (property.isAbstract) {
            if (!containingDeclaration.isAbstract && !containingDeclaration.isSealed && !inEnumClass(containingDeclaration)) {
                property.source?.let { source ->
                    reporter.report(source, FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS)
                    return
                }
            }

            if (property.delegate != null) {
                property.delegate!!.source?.let {
                    if (inInterface(containingDeclaration)) {
                        reporter.report(FirErrors.DELEGATED_PROPERTY_IN_INTERFACE.on(it, property.delegate!!))
                    } else {
                        reporter.report(FirErrors.ABSTRACT_DELEGATED_PROPERTY.on(it, property.delegate!!))
                    }
                }
            }

            checkAccessor(property.getter, property.delegate) { src, symbol ->
                reporter.report(FirErrors.ABSTRACT_PROPERTY_WITH_GETTER.on(src, symbol))
            }
            checkAccessor(property.setter, property.delegate) { src, symbol ->
                if (symbol.fir.visibility == Visibilities.Private && property.visibility != Visibilities.Private) {
                    reporter.report(FirErrors.PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY.on(src, symbol))
                } else {
                    reporter.report(FirErrors.ABSTRACT_PROPERTY_WITH_SETTER.on(src, symbol))
                }
            }
        }

        checkPropertyInitializer(containingDeclaration, property, reporter)

        if (property.isOpen) {
            checkAccessor(property.setter, property.delegate) { src, symbol ->
                if (symbol.fir.visibility == Visibilities.Private && property.visibility != Visibilities.Private) {
                    reporter.report(FirErrors.PRIVATE_SETTER_FOR_OPEN_PROPERTY.on(src, symbol))
                }
            }
        }
    }

    private fun checkPropertyInitializer(containingDeclaration: FirRegularClass, property: FirProperty, reporter: DiagnosticReporter) {
        property.initializer?.source?.let {
            if (property.isAbstract) {
                reporter.report(FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER.on(it, property.initializer!!))
            } else if (inInterface(containingDeclaration)) {
                reporter.report(FirErrors.PROPERTY_INITIALIZER_IN_INTERFACE.on(it, property.initializer!!))
            }
        }
        if (property.isAbstract) {
            if (property.initializer == null && property.delegate == null && property.returnTypeRef is FirImplicitTypeRef) {
                property.source?.let {
                    reporter.report(FirErrors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(it, property.symbol))
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

    private fun inInterface(containingDeclaration: FirRegularClass): Boolean =
        containingDeclaration.classKind == ClassKind.INTERFACE

    private fun inEnumClass(containingDeclaration: FirRegularClass): Boolean =
        containingDeclaration.classKind == ClassKind.ENUM_CLASS
}
