/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeUnderscoreUsageWithoutBackticks
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.stubs.elements.KtNameReferenceExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtTypeProjectionElementType

object FirReservedUnderscoreExpressionChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        when (expression) {
            is FirResolvable -> {
                reportIfUnderscore(expression.calleeReference.source, context, reporter, isExpression = true)
            }
            is FirResolvedQualifier -> {
                for (reservedUnderscoreDiagnostic in expression.nonFatalDiagnostics.filterIsInstance<ConeUnderscoreUsageWithoutBackticks>()) {
                    reporter.reportOn(reservedUnderscoreDiagnostic.source, FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS, context)
                }
            }
        }
    }
}

object FirReservedUnderscoreDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration is FirRegularClass ||
            declaration is FirTypeParameter ||
            declaration is FirProperty ||
            declaration is FirTypeAlias
        ) {
            reportIfUnderscore(declaration, context, reporter)
        } else if (declaration is FirFunction) {
            if (declaration is FirSimpleFunction) {
                reportIfUnderscore(declaration, context, reporter)
            }

            val isSingleUnderscoreAllowed = declaration is FirAnonymousFunction || declaration is FirPropertyAccessor
            for (parameter in declaration.valueParameters) {
                reportIfUnderscore(
                    parameter,
                    context,
                    reporter,
                    isSingleUnderscoreAllowed = isSingleUnderscoreAllowed
                )
            }
        } else if (declaration is FirFile) {
            for (import in declaration.imports) {
                reportIfUnderscore(import.aliasSource, context, reporter, isExpression = false)
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
    val declarationSource = declaration.source
    val rawName = when (declarationSource) {
        is FirPsiSourceElement ->
            (declarationSource.psi as? PsiNameIdentifierOwner)?.nameIdentifier?.text
        is FirLightSourceElement ->
            declarationSource.treeStructure.nameIdentifier(declarationSource.lighterASTNode)?.toString()
        else ->
            null
    }

    reportIfUnderscore(rawName, declarationSource, context, reporter, isSingleUnderscoreAllowed)

    val returnOrReceiverTypeRef = when (declaration) {
        is FirValueParameter -> declaration.returnTypeRef
        is FirFunction -> declaration.receiverTypeRef
        else -> null
    }

    if (returnOrReceiverTypeRef is FirResolvedTypeRef) {
        val delegatedTypeRef = returnOrReceiverTypeRef.delegatedTypeRef
        if (delegatedTypeRef is FirUserTypeRef) {
            for (qualifierPart in delegatedTypeRef.qualifier) {
                reportIfUnderscore(qualifierPart.source, context, reporter, isExpression = true)

                for (typeArgument in qualifierPart.typeArgumentList.typeArguments) {
                    reportIfUnderscore(typeArgument.source, context, reporter, isExpression = true)
                }
            }
        }
    }
}

private fun reportIfUnderscore(
    source: FirSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isExpression: Boolean
) {
    if (source != null && source.kind !is FirFakeSourceElementKind) {
        var rawName: String? = null
        if (source is FirPsiSourceElement) {
            val psi = source.psi
            rawName = if (psi is KtNameReferenceExpression) {
                psi.getReferencedNameElement().node.text
            } else if (psi is KtTypeProjection) {
                psi.typeReference?.typeElement?.text
            } else if (psi is LeafPsiElement && psi.elementType == KtTokens.IDENTIFIER) {
                psi.text
            } else {
                null
            }
        } else if (source is FirLightSourceElement) {
            val tokenType = source.elementType
            rawName = if (tokenType is KtNameReferenceExpressionElementType || tokenType == KtTokens.IDENTIFIER) {
                source.lighterASTNode.toString()
            } else if (tokenType is KtTypeProjectionElementType) {
                source.lighterASTNode.getChildren(source.treeStructure).last().toString()
            } else {
                null
            }
        }

        if (rawName?.isUnderscore == true) {
            reporter.reportOn(
                source,
                if (isExpression) FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS else FirErrors.UNDERSCORE_IS_RESERVED,
                context
            )
        }
    }
}

private fun reportIfUnderscore(
    rawName: CharSequence?,
    source: FirSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isSingleUnderscoreAllowed: Boolean
) {
    if (source == null || rawName == null || isSingleUnderscoreAllowed && rawName == "_") {
        return
    }

    if (rawName.isUnderscore && source.kind !is FirFakeSourceElementKind) {
        reporter.reportOn(
            source,
            FirErrors.UNDERSCORE_IS_RESERVED,
            context
        )
    }
}

val CharSequence.isUnderscore: Boolean
    get() = all { it == '_' }

