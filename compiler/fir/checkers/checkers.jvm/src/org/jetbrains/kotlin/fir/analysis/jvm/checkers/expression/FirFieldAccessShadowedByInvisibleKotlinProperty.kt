/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature.ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toClassSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.visibilityChecker

object FirFieldAccessShadowedByInvisibleKotlinProperty : FirPropertyAccessExpressionChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirPropertyAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        checkFieldAccess(expression, context, reporter)
    }
}

object FirFieldReferenceShadowedByInvisibleKotlinProperty : FirCallableReferenceAccessChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirCallableReferenceAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        checkFieldAccess(expression, context, reporter)
    }
}

private fun checkFieldAccess(
    expression: FirQualifiedAccessExpression,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    if (context.languageVersionSettings.supportsFeature(ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty)) return
    val fieldSymbol = expression.toResolvedCallableSymbol() as? FirFieldSymbol ?: return
    if (!fieldSymbol.isJavaOrEnhancement) return

    val containingFile = context.containingFile ?: return
    val dispatchReceiver = expression.dispatchReceiver ?: return
    checkClashWithInvisibleProperty(context, fieldSymbol, containingFile, dispatchReceiver, reporter, expression)
    checkClashWithCompanionProperty(context, fieldSymbol, reporter, expression)
}

private fun checkClashWithInvisibleProperty(
    context: CheckerContext,
    fieldSymbol: FirFieldSymbol,
    containingFile: FirFile,
    dispatchReceiver: FirExpression,
    reporter: DiagnosticReporter,
    expression: FirQualifiedAccessExpression,
) {
    val scope = dispatchReceiver.resolvedType.scope(
        context.session,
        context.scopeSession,
        CallableCopyTypeCalculator.Forced,
        requiredMembersPhase = null
    ) ?: return
    val properties = scope.getProperties(fieldSymbol.name)
    for (property in properties) {
        if (property !is FirPropertySymbol) continue
        if (!property.hasBackingField) continue
        val isVisible = context.session.visibilityChecker.isVisible(
            property,
            context.session,
            containingFile,
            context.containingDeclarations,
            dispatchReceiver,
        )
        if (!isVisible) {
            reporter.reportOn(expression.source, JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY, property, context)
            break
        }
    }
}

private fun checkClashWithCompanionProperty(
    context: CheckerContext,
    fieldSymbol: FirFieldSymbol,
    reporter: DiagnosticReporter,
    expression: FirQualifiedAccessExpression,
) {
    val dispatchReceiverClass = expression.dispatchReceiver?.resolvedType?.toRegularClassSymbol(context.session) ?: return
    val companionClass = dispatchReceiverClass.companionObjectSymbol ?: return
    val companionScope = companionClass.unsubstitutedScope(context)
    val properties = companionScope.getProperties(fieldSymbol.name)
    for (property in properties) {
        if (property !is FirPropertySymbol) continue
        if (property.hasBackingField) {
            reporter.reportOn(expression.source, JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY, property, context)
            break
        }
    }
}
