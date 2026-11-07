/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.js.validateQualifier
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsQualifier

object FirJsQualifierChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotation = declaration.symbol.resolvedAnnotationsWithClassIds.getAnnotationByClassId(JsQualifier, context.session) ?: return

        checkQualifierArgument(annotation)

        if (declaration is FirFile) return

        if (!declaration.symbol.isEffectivelyExternal(context.session)) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.JS_QUALIFIER_PROHIBITED_ON_NON_EXTERNAL)
        } else if (!context.isTopLevel) {
            reporter.reportOn(annotation.source, FirWebCommonErrors.JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkQualifierArgument(annotation: FirAnnotation) {
        val argument = annotation.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression ?: return
        val qualifier = argument.value as? String ?: return

        if (!validateQualifier(qualifier)) {
            reporter.reportOn(argument.source, FirWebCommonErrors.WRONG_JS_QUALIFIER)
        }
    }
}
