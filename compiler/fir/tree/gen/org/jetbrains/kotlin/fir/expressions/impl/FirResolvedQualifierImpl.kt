/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirResolvedQualifierImpl(
    override val source: FirSourceElement?,
    override var packageFqName: FqName,
    override var relativeClassFqName: FqName?
) : FirResolvedQualifier(), FirAbstractAnnotatedElement {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val classId: ClassId? get() = relativeClassFqName?.let {
    ClassId(packageFqName, it, false)
}
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirResolvedQualifierImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifierImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }
}
