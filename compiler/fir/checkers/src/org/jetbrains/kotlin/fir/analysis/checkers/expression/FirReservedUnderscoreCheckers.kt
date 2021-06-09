/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.text
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.stubs.elements.KtDotQualifiedExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtNameReferenceExpressionElementType

object FirReservedUnderscoreExpressionChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.source

        if (expression is FirFunctionCall) {
            val calleeReferenceSource = expression.calleeReference.source
            if (calleeReferenceSource is FirLightSourceElement && calleeReferenceSource.lighterASTNode.tokenType == KtNodeTypes.OPERATION_REFERENCE) {
                return
            }

            reportIfUnderscore(
                expression.calleeReference.source.text, expression.calleeReference.source, context, reporter,
                isExpression = true
            )
        } else if (expression is FirQualifiedAccess) {
            if (source is FirPsiSourceElement) {
                reportIfUnderscoreInQualifiedAccess(source, expression, context, reporter)
            } else if (source is FirLightSourceElement) {
                reportIfUnderscoreInQualifiedAccess(source, expression, context, reporter)
            }
        } else if (expression is FirGetClassCall) {
            for (argument in expression.argumentList.arguments) {
                reportIfUnderscore(argument.source.text, expression.source, context, reporter, isExpression = true)
            }
        } else if (expression is FirReturnExpression) {
            var labelName: String? = null
            if (source is FirPsiSourceElement) {
                labelName = (source.psi.parent as? KtLabeledExpression)?.getLabelName()
            } else if (source is FirLightSourceElement) {
                val parent = source.treeStructure.getParent(source.lighterASTNode)
                if (parent != null && parent.tokenType == KtNodeTypes.LABELED_EXPRESSION) {
                    labelName = source.treeStructure.findDescendantByType(parent, KtNodeTypes.LABEL).toString()
                    labelName = labelName.substring(0, labelName.length - 1)
                }
            }

            reportIfUnderscore(labelName, expression.source, context, reporter)
        }
    }

    private fun reportIfUnderscoreInQualifiedAccess(
        source: FirPsiSourceElement,
        expression: FirStatement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun processQualifiedAccess(psi: PsiElement?) {
            if (psi is KtNameReferenceExpression) {
                reportIfUnderscore(psi.text, expression.source, context, reporter, isExpression = true)
            } else if (psi is KtDotQualifiedExpression || psi is KtCallableReferenceExpression) {
                processQualifiedAccess(psi.firstChild)
                processQualifiedAccess(psi.lastChild)
            }
        }

        val psi = source.psi
        if (psi.parent !is KtDotQualifiedExpression && psi.parent !is KtCallableReferenceExpression) {
            processQualifiedAccess(psi)
        }
    }

    private fun reportIfUnderscoreInQualifiedAccess(
        source: FirLightSourceElement,
        expression: FirStatement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun processQualifiedAccess(lightSourceElement: LighterASTNode?) {
            val tokenType = lightSourceElement?.tokenType
            if (tokenType is KtNameReferenceExpressionElementType) {
                reportIfUnderscore(lightSourceElement.toString(), expression.source, context, reporter, isExpression = true)
            } else if (lightSourceElement != null && (tokenType is KtDotQualifiedExpressionElementType || tokenType == KtNodeTypes.CALLABLE_REFERENCE_EXPRESSION)) {
                val children = lightSourceElement.getChildren(source.treeStructure)
                processQualifiedAccess(children.first())
                processQualifiedAccess(children.last())
            }
        }

        val astNode = source.lighterASTNode
        val parent = source.treeStructure.getParent(astNode)
        if (parent?.tokenType !is KtDotQualifiedExpressionElementType && parent?.tokenType != KtNodeTypes.CALLABLE_REFERENCE_EXPRESSION) {
            processQualifiedAccess(astNode)
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
    val source = declaration.source
    val rawIdentifier = when (source) {
        is FirPsiSourceElement ->
            (source.psi as? PsiNameIdentifierOwner)?.nameIdentifier?.text
        is FirLightSourceElement ->
            source.treeStructure.nameIdentifier(source.lighterASTNode)?.toString()
        else ->
            null
    }

    reportIfUnderscore(rawIdentifier, source, context, reporter, isSingleUnderscoreAllowed)

    val returnOrReceiverTypeRef = when (declaration) {
        is FirValueParameter -> declaration.returnTypeRef.source
        is FirFunction<*> -> declaration.receiverTypeRef?.source
        else -> null
    }

    val isReportUnderscoreInReturnOrReceiverTypeRef = when (returnOrReceiverTypeRef) {
        is FirPsiSourceElement -> {
            val psi = returnOrReceiverTypeRef.psi
            psi is KtTypeReference && psi.anyDescendantOfType<LeafPsiElement> { isUnderscore(it.text) }
        }
        is FirLightSourceElement -> {
            val lighterASTNode = returnOrReceiverTypeRef.lighterASTNode
            lighterASTNode.tokenType == KtNodeTypes.TYPE_REFERENCE &&
                    source?.treeStructure?.findFirstDescendant(lighterASTNode)
                    { it.tokenType == KtNodeTypes.REFERENCE_EXPRESSION && isUnderscore(it.toString()) } != null
        }
        else -> {
            false
        }
    }

    if (isReportUnderscoreInReturnOrReceiverTypeRef) {
        reporter.reportOn(returnOrReceiverTypeRef, FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS, context)
    }
}

private fun reportIfUnderscore(
    text: CharSequence?,
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

private fun isUnderscore(text: CharSequence) = text.all { it == '_' }
