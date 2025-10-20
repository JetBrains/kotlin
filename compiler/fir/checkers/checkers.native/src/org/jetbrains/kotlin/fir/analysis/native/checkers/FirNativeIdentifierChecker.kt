/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name

object FirNativeIdentifierChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    // Also includes characters used by IR mangler (see MangleConstant).
    private val invalidChars = setOf(
        '.', ';', ',', '(', ')', '[', ']', '{', '}', '/', '<', '>',
        ':', '\\', '$', '&', '~', '*', '?', '#', '|', '§', '%', '@',
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val source = declaration.source
        when (declaration) {
            is FirRegularClass -> checkNameAndReport(declaration.name, source)
            is FirNamedFunction -> checkNameAndReport(declaration.name, source)
            is FirTypeParameter -> checkNameAndReport(declaration.name, source)
            is FirProperty -> checkNameAndReport(declaration.name, source)
            is FirTypeAlias -> checkNameAndReport(declaration.name, source)
            is FirValueParameter -> checkNameAndReport(declaration.name, source)
            is FirEnumEntry -> checkNameAndReport(declaration.name, source)
            else -> return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkNameAndReport(
        name: Name,
        source: KtSourceElement?,
    ) {
        if (source != null && source.kind !is KtFakeSourceElementKind && !name.isSpecial) {
            val text = name.asString()
            val message = when {
                text.isEmpty() -> "should not be empty"
                text.any { it in invalidChars } -> "contains illegal characters: " +
                        invalidChars.intersect(text.toSet()).joinToString("", prefix = "\"", postfix = "\"")
                else -> null
            }

            if (message != null) {
                reporter.reportOn(source, FirNativeErrors.INVALID_CHARACTERS_NATIVE, message)
            }
        }
    }
}
