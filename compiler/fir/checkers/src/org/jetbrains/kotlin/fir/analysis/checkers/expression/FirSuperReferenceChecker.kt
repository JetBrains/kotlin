/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes.TYPE_ARGUMENT_LIST
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.TYPE_ELEMENT_TYPES
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSuperReferenceChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val superReference = expression.calleeReference.safeAs<FirSuperReference>()?.takeIf { it.hadExplicitTypeInSource() } ?: return

        val typeArgumentListSource = superReference.superTypeRef.source?.getChild(TYPE_ELEMENT_TYPES)?.getChild(TYPE_ARGUMENT_LIST)
        val superType = superReference.superTypeRef.coneType
        if (typeArgumentListSource != null && superType !is ConeKotlinErrorType && superType.typeArguments.all { it !is ConeKotlinErrorType }) {
            reporter.reportOn(typeArgumentListSource, FirErrors.TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER, context)
        }
    }
}
