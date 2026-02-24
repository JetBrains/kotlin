/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.isArrayOfFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind

context(context: CheckerContext)
fun DiagnosticReporter.reportIfHasAnnotation(
    declaration: FirDeclaration,
    annotationClassId: ClassId,
    error: KtDiagnosticFactory0
) {
    val annotation = declaration.getAnnotationByClassId(annotationClassId, context.session)
    if (annotation != null) {
        reportOn(annotation.source, error)
    }
}

fun FirRegularClassSymbol.forwardDeclarationKindOrNull(): NativeForwardDeclarationKind? =
    NativeForwardDeclarationKind.packageFqNameToKind[classId.packageFqName]

/**
 * FIR analogue of IR helper `tryGetIntrinsicType(callSite: IrFunctionAccessExpression)`.
 * Tries to read `@kotlin.native.internal.TypedIntrinsic(kind: String)` from the called function
 * and convert it to `IntrinsicType`.
 */
fun tryGetIntrinsicType(callSite: FirFunctionCall): IntrinsicType? {
    val symbol = callSite.toResolvedCallableSymbol() ?: return null
    val session = symbol.moduleData.session

    val annotation = symbol.getAnnotationByClassId(ClassId.topLevel(KonanFqNames.typedIntrinsic), session)
        ?: return null

    val literal = annotation.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression
    val value = literal?.value as? String ?: return null

    return runCatching { IntrinsicType.valueOf(value) }.getOrNull()
}

fun FirFunctionCall.isArrayOfCall(session: FirSession): Boolean {
    return (toResolvedCallableSymbol() as? FirNamedFunctionSymbol)?.isArrayOfFunction(session, this.argumentList) == true
}
