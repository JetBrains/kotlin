/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirAnnotationCall : FirExpression(), FirCall {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val argumentList: FirArgumentList
    abstract val useSiteTarget: AnnotationUseSiteTarget?
    abstract val annotationTypeRef: FirTypeRef
    abstract val resolved: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitAnnotationCall(this, data)

    abstract fun replaceResolved(newResolved: Boolean)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnnotationCall
}
