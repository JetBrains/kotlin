/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractFirReflectionApiCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

class FirWebReflectionAPICallChecker(val isWasm: Boolean) : AbstractFirReflectionApiCallChecker() {
    context(context: CheckerContext)
    override fun isWholeReflectionApiAvailable(): Boolean {
        return false
    }

    context(context: CheckerContext)
    override fun isAllowedKClassMember(name: Name): Boolean {
        return super.isAllowedKClassMember(name) || name == K_CLASS_IS_INTERFACE_NAME
    }

    context(context: CheckerContext)
    override fun isAllowedReflectionApi(name: Name, containingClassId: ClassId): Boolean {
        return super.isAllowedReflectionApi(name, containingClassId) ||
                containingClassId in StandardClassIds.Annotations.associatedObjectAnnotations ||
                name == StandardNames.FqNames.findAssociatedObject.shortName()
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun report(source: KtSourceElement?) {
        val backend = if (isWasm) "Wasm" else "JS"
        reporter.reportOn(source, FirWebCommonErrors.UNSUPPORTED_REFLECTION_API, "This reflection API is not supported in Kotlin/$backend.")
    }
}
