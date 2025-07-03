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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.mustUseReturnValueStatusComponent
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

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
    val classId = classId ?: return false
    if (classId == StandardClassIds.Nothing) return true
    if (classId == StandardClassIds.Unit && !isMarkedNullable) return true
    if (classId == JAVA_LANG_VOID && !isMarkedNullable) return true // Void? is not ignorable just as Unit?
    return false
}

private fun FirCallableSymbol<*>.isExcluded(session: FirSession): Boolean = session.mustUseReturnValueStatusComponent.hasIgnorableLikeAnnotation(resolvedAnnotationClassIds)

private fun FirCallableSymbol<*>.isSubjectToCheck(): Boolean {
    // TODO KT-71195 : treating everything in kotlin. seems to be the easiest way to handle builtins, FunctionN, etc..
    // This should be removed after bootstrapping and recompiling stdlib in FULL mode
    if (this.callableId?.packageName?.asString() == "kotlin") return this.origin !is FirDeclarationOrigin.Enhancement
    callableId?.ifMappedTypeCollection { return it }


    // TBD: Do we want to report them unconditionally? Or only in FULL mode?
    // If latter, metadata flag should be added for them too.
    if (this is FirEnumEntrySymbol) return true

    return resolvedStatus.hasMustUseReturnValue
}

// TODO: after kotlin.collections package will be bootstrapped and @MustUseReturnValue-annotated,
// this list should contain only typealiased Java types (HashSet, StringBuilder, etc.)
private inline fun CallableId.ifMappedTypeCollection(nonIgnorableCollectionMethod: (Boolean) -> Unit) {
    val packageName = packageName.asString()
    if (packageName != "kotlin.collections" && packageName != "java.util") return
    val className = className?.asString() ?: return
    if (className !in setOf(
            "Collection",
            "MutableCollection",
            "List",
            "MutableList",
            "ArrayList",
            "Set",
            "MutableSet",
            "HashSet",
            "LinkedHashSet",
            "Map",
            "MutableMap",
            "HashMap",
            "LinkedHashMap",
            "ArrayDeque"
        )
    ) return
    nonIgnorableCollectionMethod(
        callableName.asString() !in setOf(
            "add",
            "addAll",
            "remove",
            "removeAt",
            "removeAll",
            "set",
            "put",
            "retainAll",
            "removeLast"
        )
    )
}
