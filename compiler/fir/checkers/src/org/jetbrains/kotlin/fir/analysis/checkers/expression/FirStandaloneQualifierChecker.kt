/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.checkers.isStandalone
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType

object FirStandaloneQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        if (!expression.isStandalone()) return
        if (expression.reportPackageOrNoCompanion()) return

        if (!expression.typeArguments.any { it.isExplicit }) return
        if (preForbidUselessTypeArgumentsIn25Implementation(expression)) return

        val diagnostic = when {
            LanguageFeature.ForbidUselessTypeArgumentsIn25.isEnabled() -> FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS
            else -> FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS_WARNING
        }
        reporter.reportOn(expression.source, diagnostic, "Object")
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirResolvedQualifier.reportPackageOrNoCompanion(): Boolean {
        val symbol = symbol
        if (symbol == null) {
            reporter.reportOn(source, FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND)
            return true
        }

        if (isNotResolvedToObject) {
            reporter.reportOn(source, FirErrors.NO_COMPANION_OBJECT, symbol)
            return true
        }

        return false
    }

    context(context: CheckerContext)
    private val FirResolvedQualifier.isNotResolvedToObject: Boolean
        // TODO: it'd be nice to use `resolvedToCompanionObject` here, but see KT-84299
        get() = resolvedType.isUnit && symbol?.fullyExpandedClass()?.classKind != ClassKind.OBJECT

    /**
     * Implementation before [LanguageFeature.ForbidUselessTypeArgumentsIn25] which missed some cases
     * (see KT-84280, KT-84281) of [FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS].
     * TODO: KT-84254. Once [LanguageFeature.ForbidUselessTypeArgumentsIn25] becomes obsolete, remove this implementation fully.
     *
     * @return true if [FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS] was reported.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun preForbidUselessTypeArgumentsIn25Implementation(expression: FirResolvedQualifier): Boolean {
        if (!expression.resolvedType.isUnit) {
            reporter.reportOn(expression.source, FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS, "Object")
            return true
        }
        return false
    }
}
