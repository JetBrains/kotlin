/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isComponentCall
import org.jetbrains.kotlin.fir.analysis.checkers.isDestructuringDeclaration
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeForTypeMismatch
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INITIALIZER_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirInitializerTypeMismatchChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val initializer = declaration.initializer ?: return
        if (declaration.isDestructuringDeclaration) return
        if (initializer.isComponentCall) return
        val propertyType = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val expressionType = initializer.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val typeContext = context.session.typeContext

        if (!isSubtypeForTypeMismatch(typeContext, subtype = expressionType, supertype = propertyType)) {
            val source = declaration.source ?: return
            reporter.report(INITIALIZER_TYPE_MISMATCH.on(source, propertyType, expressionType), context)
        }
    }
}