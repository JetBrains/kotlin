/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind

fun DiagnosticReporter.reportIfHasAnnotation(
    declaration: FirDeclaration,
    annotationClassId: ClassId,
    error: KtDiagnosticFactory0,
    context: CheckerContext
) {
    val annotation = declaration.getAnnotationByClassId(annotationClassId, context.session)
    if (annotation != null) {
        reportOn(annotation.source, error, context)
    }
}

fun FirRegularClassSymbol.forwardDeclarationKindOrNull(): NativeForwardDeclarationKind? =
    NativeForwardDeclarationKind.packageFqNameToKind[classId.packageFqName]