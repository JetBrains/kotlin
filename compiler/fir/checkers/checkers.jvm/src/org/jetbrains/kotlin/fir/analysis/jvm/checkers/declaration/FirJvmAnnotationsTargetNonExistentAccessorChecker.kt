/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation

object FirJvmAnnotationsTargetNonExistentAccessorChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (declaration.dispatchReceiverType == null) return
        if (!Visibilities.isPrivate(declaration.visibility) && !declaration.isSpecialStaticProperty()) return

        val hasGetterWithBody = declaration.getter?.body != null
        val hasSetterWithBody = declaration.setter?.body != null

        if (hasGetterWithBody && hasSetterWithBody) return
        if (declaration.delegate != null) return

        val declarationName = declaration.name.asString()

        if (!hasGetterWithBody) {
            declaration.getter?.annotations?.forEach { reportIfNeeded(it, declarationName) }
        }

        if (!hasSetterWithBody) {
            declaration.setter?.annotations?.forEach { reportIfNeeded(it, declarationName) }
            declaration.setter?.valueParameters?.forEach { parameter ->
                parameter.annotations.forEach { reportIfNeeded(it, declarationName) }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIfNeeded(annotation: FirAnnotation, declarationName: String) {
        val annotationClass = annotation.toAnnotationClassLikeSymbol(context.session) ?: return
        if (annotationClass.getAnnotationRetention(context.session) == AnnotationRetention.SOURCE) return

        reporter.reportOn(annotation.source, FirJvmErrors.ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR, declarationName)
    }

    context(context: CheckerContext)
    private fun FirProperty.isSpecialStaticProperty(): Boolean {
        return isConst || hasJvmFieldAnnotation(context.session)
    }
}
