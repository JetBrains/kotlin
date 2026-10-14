/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.name.ClassId

/**
 * Reports each annotation of [declaration] from [incompatibleAnnotations] on its own source.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun reportIncompatibleAnnotations(
    declaration: FirDeclaration,
    incompatibleAnnotations: Set<ClassId>,
    diagnostic: KtDiagnosticFactory1<String>,
) {
    for (annotation in declaration.annotations) {
        val classId = annotation.toAnnotationClassId(context.session) ?: continue
        if (classId !in incompatibleAnnotations) continue
        reporter.reportOn(annotation.source, diagnostic, classId.relativeClassName.asString())
    }
}
