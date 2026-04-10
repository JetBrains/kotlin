/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.types.hasResolvedType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid

object FirMultipleReferencesChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (LanguageFeature.ImprovedAliasTracking.isDisabled()) return
        FirMultipleReferencesTracker(context, reporter).visitElement(declaration.body ?: return)
    }

    class FirMultipleReferencesTracker(
        val context: CheckerContext,
        val reporter: DiagnosticReporter,
        val alreadyReported: MutableSet<List<String>> = mutableSetOf(),
    ) : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
            qualifiedAccessExpression.acceptChildren(this)

            if (qualifiedAccessExpression.hasResolvedType && qualifiedAccessExpression.resolvedType.isPrimitiveOrNullablePrimitive) return
            val references = qualifiedAccessExpression.domainReferences?.map { it.first } ?: return
            val problem = when {
                references in alreadyReported -> null
                references.size < 2 -> null
                references.size == 2 -> FirErrors.TWO_REFERENCES
                else -> FirErrors.MULTIPLE_REFERENCES
            }
            if (problem != null) {
                alreadyReported.add(references)
                reporter.reportOn(qualifiedAccessExpression.source, problem, references, context)
            }
        }
    }
}
