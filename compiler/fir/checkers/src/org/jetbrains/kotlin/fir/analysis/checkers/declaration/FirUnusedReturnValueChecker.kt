/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenSymbolsSafe
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.hasError
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ReturnValueStatus

object FirReturnValueOverrideChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.DISABLED) return

        // Only check mustUse overrides:
        if (!declaration.isOverride) return
        if (declaration.status.returnValueStatus != ReturnValueStatus.MustUse) return
        val symbol = declaration.symbol

        // Check if any of the overridden symbols have @IgnorableReturnValue
        val overriddenSymbols = symbol.directOverriddenSymbolsSafe()
        val ignorableBaseSymbol = overriddenSymbols.find {
            it.resolvedStatus.returnValueStatus == ReturnValueStatus.ExplicitlyIgnorable
                        // FIXME (KT-79923): Checking annotation is required only for tests to pass with bootstrap (old metadata) stdlib
                        // Should be deleted after re-bootstrapping stdlib again
                    || context.session.mustUseReturnValueStatusComponent.hasIgnorableLikeAnnotation(it.resolvedAnnotationClassIds)
        } ?: return

        // Report error if an overridden symbol has @IgnorableReturnValue but the current declaration doesn't
        val containingClass = ignorableBaseSymbol.getContainingClassSymbol()
            ?: error("Overridden symbol ${ignorableBaseSymbol.callableId} does not have containing class symbol")
        reporter.reportOn(
            declaration.source,
            FirErrors.OVERRIDING_IGNORABLE_WITH_MUST_USE,
            symbol,
            containingClass,
        )
    }
}

object FirReturnValueAnnotationsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private fun FirAnnotation.isMustUseReturnValue(session: FirSession): Boolean =
        toAnnotationClassId(session) == StandardClassIds.Annotations.MustUseReturnValue

    private fun FirAnnotation.isIgnorableValue(session: FirSession): Boolean =
        toAnnotationClassId(session) == StandardClassIds.Annotations.IgnorableReturnValue

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED) return

        val session = context.session
        declaration.annotations.forEach { annotation ->
            if (annotation.isMustUseReturnValue(session) || annotation.isIgnorableValue(session)) {
                reporter.reportOn(
                    annotation.source,
                    FirErrors.IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED
                )
            }
        }
    }
}


object FirUnusedReturnValueChecker : FirUnusedCheckerBase() {
    context(context: CheckerContext)
    override fun isEnabled(): Boolean =
        context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun reportUnusedExpressionIfNeeded(
        expression: FirExpression,
        hasSideEffects: Boolean,
        source: KtSourceElement?,
    ): Boolean {
        if (!hasSideEffects) return false // Do not report anything FirUnusedExpressionChecker already reported

        // Ignore Unit or Nothing
        if (expression.resolvedType.isIgnorable()) return false

        val calleeReference = expression.toReference(context.session)
        val resolvedReference = calleeReference?.resolved

        val resolvedSymbol = resolvedReference?.toResolvedCallableSymbol()?.originalOrSelf()

        if (resolvedSymbol != null && !resolvedSymbol.isSubjectToCheck()) return false

        if (resolvedSymbol?.isExcluded(context.session) == true) return false

        // Special case for `x[y] = z` assigment:
        if ((expression is FirFunctionCall) && expression.origin == FirFunctionCallOrigin.Operator && resolvedSymbol?.name?.asString() == "set") return false

        val functionName = resolvedSymbol?.name

        reporter.reportOn(
            expression.source,
            FirErrors.RETURN_VALUE_NOT_USED,
            functionName
        )
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun createVisitor(): UsageVisitorBase =
        UsageVisitor(context, reporter)

    private class UsageVisitor(
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) : UsageVisitorBase(context, reporter) {
        override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: UsageState) {
            elvisExpression.lhs.accept(this, data)
            elvisExpression.rhs.accept(this, data)
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: UsageState) {
            safeCallExpression.selector.accept(this, data)
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: UsageState) {
            checkNotNullCall.argument.accept(this, data)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: UsageState) {
            typeOperatorCall.arguments.forEach { it.accept(this, data) }
        }
    }

}

private val JAVA_LANG_VOID = ClassId.topLevel(FqName("java.lang.Void"))

private fun ConeKotlinType.isIgnorable(): Boolean {
    if (this is ConeErrorType || this.hasError()) return true
    val classId = classId ?: return false
    if (classId == StandardClassIds.Nothing) return true
    if (classId == StandardClassIds.Unit && !isMarkedNullable) return true
    if (classId == JAVA_LANG_VOID && !isMarkedNullable) return true // Void? is not ignorable just as Unit?
    return false
}

private fun FirCallableSymbol<*>.isExcluded(session: FirSession): Boolean = session.mustUseReturnValueStatusComponent.hasIgnorableLikeAnnotation(resolvedAnnotationClassIds)

private fun FirCallableSymbol<*>.isSubjectToCheck(): Boolean {
    // TBD: Do we want to report them unconditionally? Or only in FULL mode?
    // If latter, metadata flag should be added for them too.
    if (this is FirEnumEntrySymbol) return true

    return resolvedStatus.returnValueStatus == ReturnValueStatus.MustUse
}
