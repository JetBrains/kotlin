/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// TODO (KT-60899): implement this checker for JS, similarly to K1's JsReflectionAPICallChecker.
abstract class AbstractFirReflectionApiCallChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    protected abstract fun isWholeReflectionApiAvailable(): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected abstract fun report(source: KtSourceElement?)

    context(context: CheckerContext)
    protected open fun isAllowedKClassMember(name: Name): Boolean = when (name) {
        K_CLASS_SIMPLE_NAME, K_CLASS_IS_INSTANCE -> true
        K_CLASS_QUALIFIED_NAME -> context.languageVersionSettings.getFlag(AnalysisFlags.allowFullyQualifiedNameInKClass)
        else -> false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    final override fun check(expression: FirStatement) {
        if (isWholeReflectionApiAvailable()) return

        // Do not report the diagnostic on kotlin-reflect sources.
        if (isReflectionSource()) return

        val resolvedReference = expression.toReference(context.session)?.resolved ?: return
        val referencedSymbol = resolvedReference.resolvedSymbol as? FirCallableSymbol ?: return

        val containingClassId = (expression as? FirQualifiedAccessExpression)?.dispatchReceiver?.resolvedType?.fullyExpandedClassId(context.session)
        if (containingClassId == null || containingClassId.packageFqName != StandardNames.KOTLIN_REFLECT_FQ_NAME) return

        if (!isAllowedReflectionApi(referencedSymbol.name, containingClassId)) {
            report(resolvedReference.source ?: expression.source)
        }
    }

    context(context: CheckerContext)
    protected open fun isAllowedReflectionApi(name: Name, containingClassId: ClassId): Boolean =
        name in ALLOWED_MEMBER_NAMES ||
                containingClassId == K_CLASS && isAllowedKClassMember(name) ||
                (name.asString() == "get" || name.asString() == "set") && containingClassId in K_PROPERTY_CLASSES ||
                containingClassId in ALLOWED_CLASSES

    context(context: CheckerContext)
    private fun isReflectionSource(): Boolean {
        val containingFile = context.containingFileSymbol
        return containingFile != null && containingFile.packageFqName.startsWith(StandardNames.KOTLIN_REFLECT_FQ_NAME)
    }

    companion object {
        private val K_CLASS = ClassId.topLevel(StandardNames.FqNames.kClass.toSafe())

        private val K_CLASS_SIMPLE_NAME = Name.identifier("simpleName")
        private val K_CLASS_IS_INSTANCE = Name.identifier("isInstance")
        private val K_CLASS_QUALIFIED_NAME = Name.identifier("qualifiedName")

        @JvmStatic
        protected val K_CLASS_IS_INTERFACE_NAME: Name = Name.identifier("isInterface")

        private val K_PROPERTY_CLASSES: Set<ClassId> =
            listOf(
                StandardNames.FqNames.kProperty0,
                StandardNames.FqNames.kProperty1,
                StandardNames.FqNames.kProperty2,
                StandardNames.FqNames.kMutableProperty0,
                StandardNames.FqNames.kMutableProperty1,
                StandardNames.FqNames.kMutableProperty2,
            ).mapTo(HashSet()) { ClassId.topLevel(it.toSafe()) }

        private val ALLOWED_MEMBER_NAMES: Set<Name> =
            listOf("equals", "hashCode", "toString", "invoke", "name").mapTo(HashSet(), Name::identifier)

        private val ALLOWED_CLASSES: Set<ClassId> =
            listOf("KType", "KTypeParameter", "KTypeProjection", "KTypeProjection.Companion", "KVariance").mapTo(HashSet()) {
                ClassId(StandardNames.KOTLIN_REFLECT_FQ_NAME, FqName(it), isLocal = false)
            }
    }
}
