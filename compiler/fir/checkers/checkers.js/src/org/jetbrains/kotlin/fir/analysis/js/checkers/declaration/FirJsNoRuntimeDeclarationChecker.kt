/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsNoRuntimeDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotation = declaration.findAnnotation(JsStandardClassIds.Annotations.JsNoRuntime, context.session) ?: return

        if (declaration is FirClass && declaration.isInterface) {
            if (declaration.isExternal) {
                reporter.reportOn(annotation.source ?: declaration.source, FirJsErrors.JS_NO_RUNTIME_USELESS_ON_EXTERNAL_INTERFACE)
            }
            return
        }

        val targetSource: KtSourceElement? = annotation.source ?: declaration.source
        reporter.reportOn(targetSource, FirJsErrors.JS_NO_RUNTIME_WRONG_TARGET)
    }

    private fun FirDeclaration.findAnnotation(classId: ClassId, session: FirSession): FirAnnotation? {
        return annotations.firstOrNull {
            it.annotationTypeRef.coneType.fullyExpandedClassId(session) == classId
        }
    }
}
