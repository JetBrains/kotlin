/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedQualifier : FirPureAbstractElement(), FirExpression {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    abstract val classId: ClassId?
    abstract val typeArguments: List<FirTypeProjection>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitResolvedQualifier(this, data)

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifier
}
