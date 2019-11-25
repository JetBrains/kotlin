/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.expressions.impl.FirConstExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorLoopImpl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId

inline val FirAnnotationCall.coneClassLikeType: ConeClassLikeType?
    get() = ((annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)

inline val FirAnnotationCall.classId: ClassId?
    get() = coneClassLikeType?.lookupTag?.classId

fun <T> FirConstExpressionImpl(source: FirSourceElement?, kind: IrConstKind<T>, value: T?, diagnostic: FirDiagnostic): FirExpression =
    value?.let { FirConstExpressionImpl(source, kind, it) } ?: FirErrorExpressionImpl(source, diagnostic)

inline val FirTypeOperatorCall.argument: FirExpression get() = arguments.first()

fun FirExpression.toResolvedCallableReference(): FirResolvedNamedReference? {
    return (this as? FirQualifiedAccess)?.calleeReference as? FirResolvedNamedReference
}

fun FirExpression.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as FirCallableSymbol<*>?
}

fun FirErrorLoop(source: FirSourceElement?, diagnostic: FirDiagnostic): FirErrorLoop {
    return FirErrorLoopImpl(source, diagnostic).apply {
        condition = FirErrorExpressionImpl(source, diagnostic)
    }
}
