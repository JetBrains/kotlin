/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.BaseTransformedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.FqName

@BaseTransformedType
interface FirAnnotationCall : FirCall {
    val annotationTypeRef: FirTypeRef

    // May be should be not-null (with correct default target)
    val useSiteTarget: AnnotationUseSiteTarget?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnnotationCall(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotationTypeRef.accept(visitor, data)
        super.acceptChildren(visitor, data)
    }
}

val FirAnnotationCall.coneClassLikeType: ConeClassLikeType?
    get() = ((annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)?.takeIf { it !is ConeClassErrorType }

val FirAnnotationCall.resolvedFqName: FqName?
    get() = coneClassLikeType?.lookupTag?.classId?.asSingleFqName()
