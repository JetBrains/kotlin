/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.load.kotlin.JvmClassFileBacked
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement

object FirJvmInlineTargetQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return

        val isInline = when (callableSymbol) {
            is FirFunctionSymbol<*> -> callableSymbol.isInline
            is FirPropertySymbol -> {
                val accessor = if (expression.isLhsOfAssignment()) callableSymbol.setterSymbol else callableSymbol.getterSymbol
                accessor != null && accessor.isInline
            }
            else -> false
        }

        if (isInline) {
            checkInlineTargetVersion(callableSymbol, expression)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInlineTargetVersion(
        callableSymbol: FirCallableSymbol<*>,
        element: FirElement,
    ) {
        val currentJvmTarget = context.session.jvmTargetProvider?.jvmTarget ?: return

        // The inline body belongs to the original declaration, not to a fake override in a subclass.
        val declarationSymbol = callableSymbol.unwrapFakeOverrides()
        val inlinedVersion = if (declarationSymbol.origin.fromSource) {
            // A source dependency module analyzed by the IDE is compiled with its own JVM target.
            // In one compilation, all source modules share the JVM target.
            declarationSymbol.moduleData.session.jvmTargetProvider?.jvmTarget?.majorVersion
        } else {
            declarationSymbol.classFileMajorVersion()
        } ?: return

        val currentVersion = currentJvmTarget.majorVersion

        if (currentVersion < inlinedVersion) {
            reporter.reportOn(
                element.toReference(context.session)?.source ?: element.source,
                FirJvmErrors.INLINE_FROM_HIGHER_PLATFORM,
                JvmTarget.getDescription(inlinedVersion),
                JvmTarget.getDescription(currentVersion)
            )
        }
    }

    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.classFileMajorVersion(): Int? {
        val containingClass = containingClassLookupTag()
        // Stub-based library declarations in the Analysis API have no binary class, so their sources provide the version themselves
        val binaryEntity = if (containingClass != null) {
            val containingClassSymbol = containingClass.toRegularClassSymbol() ?: return null

            @OptIn(SymbolInternals::class)
            when (val sourceElement = containingClassSymbol.fir.sourceElement) {
                is KotlinJvmBinarySourceElement -> sourceElement.binaryClass
                else -> sourceElement
            }
        } else {
            when (val containerSource = containerSource) {
                is JvmPackagePartSource -> containerSource.knownJvmBinaryClass
                else -> containerSource
            }
        }

        return (binaryEntity as? JvmClassFileBacked)?.classFileMajorVersion
    }
}
