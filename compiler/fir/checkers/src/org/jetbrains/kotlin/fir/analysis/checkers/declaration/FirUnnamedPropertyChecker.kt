/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.name.SpecialNames

object FirUnnamedPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (declaration.name != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
            return
        }

        val isDesugaredComponentCall = declaration.source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY

        if (declaration.isVar && !isDesugaredComponentCall) {
            reporter.reportOn(declaration.source, FirErrors.UNNAMED_VAR_PROPERTY)
        }

        if (declaration.delegate != null) {
            reporter.reportOn(declaration.delegate?.source, FirErrors.UNNAMED_DELEGATED_PROPERTY)
        }

        if (!isDesugaredComponentCall && declaration.isCatchParameter != true) {
            declaration.requireFeatureSupport(
                LanguageFeature.UnnamedLocalVariables,
                positioningStrategy = SourceElementPositioningStrategies.NAME_IDENTIFIER,
            )
        }

        if (declaration.initializer == null && declaration.delegate == null && declaration.isCatchParameter != true) {
            reporter.reportOn(declaration.source, FirErrors.MUST_BE_INITIALIZED)
        }
    }
}
