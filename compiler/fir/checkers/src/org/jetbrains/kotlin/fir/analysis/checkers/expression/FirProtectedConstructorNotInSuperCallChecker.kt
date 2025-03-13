/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.originalIfFakeOverride
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.util.PrivateForInline

object FirProtectedConstructorNotInSuperCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference.resolved ?: return
        val symbol = reference.toResolvedConstructorSymbol() ?: return
        val constructedClass = symbol.getConstructedClass(context.session)

        if (
            !shouldAllowSuchCallNonetheless(symbol, context) &&
            symbol.visibility.normalize() == Visibilities.Protected &&
            // Prevent reporting for already invisible references
            !reference.isError() &&
            context.containingDeclarations.none { it.symbol == constructedClass }
        ) {
            reporter.reportOn(expression.calleeReference.source, FirErrors.PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL, symbol, context)
        }
    }

    @OptIn(PrivateForInline::class, DirectDeclarationsAccess::class)
    private fun shouldAllowSuchCallNonetheless(symbol: FirConstructorSymbol, context: CheckerContext): Boolean {
        val containingFile = context.containingFile ?: return false
        if (containingFile.declarations.singleOrNull() is FirCodeFragment) return true
        val original = symbol.originalIfFakeOverride() ?: symbol
        return original.origin.isJavaOrEnhancement && original.callableId.packageName == containingFile.packageFqName
    }
}
