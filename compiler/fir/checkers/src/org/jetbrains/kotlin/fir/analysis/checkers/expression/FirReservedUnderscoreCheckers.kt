/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

object FirReservedUnderscoreExpressionChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression is FirFunctionCall) {
            reportIfUnderscore(
                expression.calleeReference.psi?.text, expression.source, context, reporter,
                isExpression = true
            )

            for (argument in expression.arguments) {
                if (argument is FirNamedArgumentExpression) {
                    reportIfUnderscore(argument.psi?.firstChild?.text, argument.source, context, reporter)
                }
            }
        } else if (expression is FirQualifiedAccess) {
            fun processQualifiedAccess(psi: PsiElement?) {
                if (psi is KtNameReferenceExpression) {
                    reportIfUnderscore(psi.text, expression.source, context, reporter, isExpression = true)
                } else if (psi is KtDotQualifiedExpression || psi is KtCallableReferenceExpression) {
                    processQualifiedAccess(psi.firstChild)
                    processQualifiedAccess(psi.lastChild)
                }
            }

            val psi = expression.psi
            if (psi != null && psi.parent !is KtDotQualifiedExpression && psi.parent !is KtCallableReferenceExpression) {
                processQualifiedAccess(psi)
            }
        } else if (expression is FirGetClassCall) {
            for (argument in expression.argumentList.arguments) {
                reportIfUnderscore(argument.psi?.text, expression.source, context, reporter, isExpression = true)
            }
        } else if (expression is FirReturnExpression) {
            reportIfUnderscore(expression.target.labelName, expression.source, context, reporter)
        }
    }
}

object FirReservedUnderscoreDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration is FirClass<*> ||
            declaration is FirFunction<*> ||
            declaration is FirTypeParameter ||
            declaration is FirProperty ||
            declaration is FirTypeAlias
        ) {

            reportIfUnderscore(declaration, context, reporter)

            if (declaration is FirFunction<*>) {
                for (parameter in declaration.valueParameters) {
                    reportIfUnderscore(
                        parameter,
                        context,
                        reporter,
                        isSingleUnderscoreAllowed = declaration is FirAnonymousFunction || declaration is FirPropertyAccessor
                    )
                }
            }
        } else if (declaration is FirFile) {
            for (import in declaration.imports) {
                reportIfUnderscore(import.aliasName?.asString(), import.source, context, reporter)
            }
        }
    }
}

private fun reportIfUnderscore(
    declaration: FirDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isSingleUnderscoreAllowed: Boolean = false
) {
    val rawIdentifier = (declaration.psi as? PsiNameIdentifierOwner)?.nameIdentifier?.text ?: return
    reportIfUnderscore(rawIdentifier, declaration.source, context, reporter, isSingleUnderscoreAllowed)

    fun reportIfAnyDescendantIfUnderscore(typeRef: FirTypeRef?) {
        if (typeRef == null) return

        if (typeRef.psi?.anyDescendantOfType<LeafPsiElement> { isUnderscore(it.text) } == true) {
            reporter.reportOn(
                typeRef.source,
                FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS,
                context
            )
        }
    }

    if (declaration is FirValueParameter) {
        val psi = declaration.returnTypeRef.psi
        if (psi !is KtFunctionLiteral && psi !is KtParameter) {
            reportIfAnyDescendantIfUnderscore(declaration.returnTypeRef)
        }
    } else if (declaration is FirFunction<*>) {
        reportIfAnyDescendantIfUnderscore(declaration.receiverTypeRef)
    }
}

private fun reportIfUnderscore(
    text: String?,
    source: FirSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isSingleUnderscoreAllowed: Boolean = false,
    isExpression: Boolean = false
) {
    if (text == null || isSingleUnderscoreAllowed && text == "_") {
        return
    }

    if (isUnderscore(text)) {
        reporter.reportOn(
            source,
            if (isExpression) FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS else FirErrors.UNDERSCORE_IS_RESERVED,
            context
        )
    }
}

private fun isUnderscore(text: String) = text.all { it == '_' }