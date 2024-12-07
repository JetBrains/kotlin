/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractFirReflectionApiCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object FirJsReflectionAPICallChecker : AbstractFirReflectionApiCallChecker() {
    override fun isWholeReflectionApiAvailable(context: CheckerContext): Boolean {
        return false
    }

    override fun isAllowedReflectionApi(name: Name, containingClassId: ClassId, context: CheckerContext): Boolean {
        return super.isAllowedReflectionApi(name, containingClassId, context) ||
                containingClassId in StandardClassIds.Annotations.associatedObjectAnnotations ||
                name == StandardNames.FqNames.findAssociatedObject.shortName()
    }

    override fun report(source: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(source, FirErrors.UNSUPPORTED, "This reflection API is not supported yet in JavaScript", context)
    }
}
