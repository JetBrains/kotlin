/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.utils.findIsInstanceAnd

object FirFieldAccessShadowedByInvisibleKotlinProperty : FirPropertyAccessExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirPropertyAccessExpression) {
        checkFieldAccess(expression)
    }
}

object FirFieldReferenceShadowedByInvisibleKotlinProperty : FirCallableReferenceAccessChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        checkFieldAccess(expression)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkFieldAccess(
    expression: FirQualifiedAccessExpression,
) {
    if (ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty.isEnabled()) return
    val fieldSymbol = expression.toResolvedCallableSymbol() as? FirFieldSymbol ?: return
    if (!fieldSymbol.isJavaOrEnhancement) return

    val containingFile = context.containingFileSymbol ?: return
    val dispatchReceiver = expression.dispatchReceiver ?: return
    checkClashWithInvisibleProperty(fieldSymbol, containingFile, dispatchReceiver, expression)
    checkClashWithCompanionProperty(fieldSymbol, expression)
    checkClashWithCompanionBlockProperty(fieldSymbol, expression)
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkClashWithInvisibleProperty(
    fieldSymbol: FirFieldSymbol,
    containingFileSymbol: FirFileSymbol,
    dispatchReceiver: FirExpression,
    expression: FirQualifiedAccessExpression,
) {
    val scope = dispatchReceiver.resolvedType.scope(
        CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution,
        requiredMembersPhase = null
    ) ?: return
    val properties = scope.getProperties(fieldSymbol.name)
    for (property in properties) {
        if (property !is FirPropertySymbol) continue
        if (!property.hasBackingField) continue
        val isVisible = context.session.visibilityChecker.isVisible(
            property,
            context.session,
            containingFileSymbol,
            context.containingDeclarations,
            dispatchReceiver,
        )
        if (!isVisible) {
            reporter.reportOn(expression.source, JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY, property)
            break
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkClashWithCompanionProperty(
    fieldSymbol: FirFieldSymbol,
    expression: FirQualifiedAccessExpression,
) {
    val dispatchReceiverClass = expression.dispatchReceiver?.resolvedType?.toRegularClassSymbol() ?: return
    val companionClass = dispatchReceiverClass.resolvedCompanionObjectSymbol ?: return
    checkClashWithPropertyInScope(fieldSymbol, companionClass.unsubstitutedScope(), expression)
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkClashWithCompanionBlockProperty(
    fieldSymbol: FirFieldSymbol,
    expression: FirQualifiedAccessExpression,
) {
    val dispatchReceiverClass = expression.dispatchReceiver?.resolvedType?.toRegularClassSymbol() ?: return
    checkClashWithPropertyInScope(fieldSymbol, dispatchReceiverClass.staticScope(context) ?: return, expression)
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkClashWithPropertyInScope(
    fieldSymbol: FirFieldSymbol,
    scope: FirContainingNamesAwareScope,
    expression: FirQualifiedAccessExpression,
) {
    scope.getProperties(fieldSymbol.name)
        .findIsInstanceAnd<FirPropertySymbol> { it.hasBackingField }
        ?.let { reporter.reportOn(expression.source, JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY, it) }
}
