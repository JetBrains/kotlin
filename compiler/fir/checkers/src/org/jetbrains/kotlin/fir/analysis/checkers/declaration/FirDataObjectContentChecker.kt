/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isMethodOfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirDataObjectContentChecker : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
        val source = declaration.source
        if (source == null || source.kind is KtFakeSourceElementKind) return

        val containingClass = context.containingDeclarations.lastOrNull() as? FirClass ?: return
        if (containingClass.classKind != ClassKind.OBJECT || !containingClass.hasModifier(KtTokens.DATA_KEYWORD)) return

        if (declaration.symbol.isMethodOfAny && declaration.name != OperatorNameConventions.TO_STRING) {
            reporter.reportOn(source, FirErrors.DATA_OBJECT_CUSTOM_EQUALS_OR_HASH_CODE, context)
        }
    }
}
