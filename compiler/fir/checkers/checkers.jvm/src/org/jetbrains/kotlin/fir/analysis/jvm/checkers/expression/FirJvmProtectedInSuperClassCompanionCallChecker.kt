/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker

// TODO: consider what to do with it
object FirJvmProtectedInSuperClassCompanionCallChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val dispatchReceiver = when (expression) {
            is FirQualifiedAccessExpression -> expression.dispatchReceiver
            is FirVariableAssignment -> expression.dispatchReceiver
            else -> null
        } ?: return

        val dispatchClassSymbol = dispatchReceiver.resolvedType.toRegularClassSymbol(context.session) ?: return
        val calleeReference = expression.toReference(context.session)
        val resolvedSymbol = calleeReference?.toResolvedCallableSymbol() ?: return

        if (resolvedSymbol is FirPropertySymbol &&
            context.languageVersionSettings.supportsFeature(LanguageFeature.AllowAccessToProtectedFieldFromSuperCompanion)
        ) {
            if (resolvedSymbol.isConst) return

            val backingField = resolvedSymbol.backingFieldSymbol
            if (backingField != null && backingField.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID, context.session)) return
        }

        val visibility = if (resolvedSymbol is FirPropertySymbol) {
            if (expression is FirVariableAssignment)
                resolvedSymbol.setterSymbol?.visibility ?: resolvedSymbol.visibility
            else
                resolvedSymbol.getterSymbol?.visibility ?: resolvedSymbol.visibility
        } else {
            resolvedSymbol.visibility
        }
        if (visibility != Visibilities.Protected) return
        if (resolvedSymbol.hasAnnotation(StandardClassIds.Annotations.jvmStatic, context.session)) return
        if (!dispatchClassSymbol.isCompanion) return
        val companionContainingClassSymbol =
            dispatchClassSymbol.getContainingDeclaration(context.session) as? FirRegularClassSymbol ?: return

        // Called from within a derived class
        val companionContainingType = companionContainingClassSymbol.defaultType()
        if (context.findClosest<FirClass> {
                AbstractTypeChecker.isSubtypeOf(context.session.typeContext, it.symbol.defaultType(), companionContainingType)
            } == null
        ) {
            return
        }

        // Called not within the same companion object or its owner class
        if (context.findClosest<FirClass> {
                it.symbol == dispatchClassSymbol || it.symbol == companionContainingClassSymbol
            } == null
        ) {
            reporter.reportOn(calleeReference.source, FirJvmErrors.SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC, context)
        }
    }
}
