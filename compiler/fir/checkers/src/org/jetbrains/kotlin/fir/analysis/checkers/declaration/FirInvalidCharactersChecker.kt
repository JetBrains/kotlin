/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name

object FirInvalidCharactersChecker : FirBasicDeclarationChecker() {
    private val INVALID_CHARS = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source
        when (declaration) {
            is FirRegularClass -> checkNameAndReport(declaration.name, source, context, reporter)
            is FirSimpleFunction -> checkNameAndReport(declaration.name, source, context, reporter)
            is FirTypeParameter -> checkNameAndReport(declaration.name, source, context, reporter)
            is FirProperty -> checkNameAndReport(declaration.name, source, context, reporter)
            is FirTypeAlias -> checkNameAndReport(declaration.name, source, context, reporter)
            is FirValueParameter -> checkNameAndReport(declaration.name, source, context, reporter)
            else -> return
        }
    }

    private fun checkNameAndReport(name: Name, source: FirSourceElement?, context: CheckerContext, reporter: DiagnosticReporter) {
        val nameString = name.asString()
        if (source != null &&
            source.kind !is FirFakeSourceElementKind &&
            source.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION &&
            !name.isSpecial &&
            nameString.any { it in INVALID_CHARS }
        ) {
            reporter.reportOn(
                source,
                FirErrors.INVALID_CHARACTERS,
                "contains illegal characters: ${INVALID_CHARS.intersect(nameString.toSet()).joinToString("")}",
                context
            )
        }
    }
}