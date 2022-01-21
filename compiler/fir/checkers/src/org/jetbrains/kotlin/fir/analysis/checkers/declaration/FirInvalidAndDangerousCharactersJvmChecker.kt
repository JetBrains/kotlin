/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name

object FirInvalidAndDangerousCharactersJvmChecker : FirBasicDeclarationChecker() {
    // See The Java Virtual Machine Specification, section 4.7.9.1 https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1
    private val INVALID_CHARS = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

    // These characters can cause problems on Windows. '?*"|' are not allowed in file names, and % leads to unexpected env var expansion.
    private val DANGEROUS_CHARS = setOf('?', '*', '"', '|', '%')

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

    private fun checkNameAndReport(name: Name, source: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter) {
        if (source != null &&
            source.kind !is KtFakeSourceElementKind &&
            !name.isSpecial
        ) {
            val nameString = name.asString()
            if (nameString.any { it in INVALID_CHARS }) {
                reporter.reportOn(
                    source,
                    FirErrors.INVALID_CHARACTERS,
                    "contains illegal characters: ${INVALID_CHARS.intersect(nameString.toSet()).joinToString("")}",
                    context
                )
            } else if (nameString.any { it in DANGEROUS_CHARS }) {
                reporter.reportOn(
                    source,
                    FirErrors.DANGEROUS_CHARACTERS,
                    DANGEROUS_CHARS.intersect(nameString.toSet()).joinToString(""),
                    context
                )
            }
        }
    }
}