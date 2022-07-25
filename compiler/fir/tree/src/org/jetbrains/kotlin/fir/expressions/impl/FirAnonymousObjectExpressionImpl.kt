/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transform


internal class FirAnonymousObjectExpressionImpl(
    override val source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override var anonymousObject: FirAnonymousObject,
) : FirAnonymousObjectExpression() {
    override val annotations: List<FirAnnotation>
        get() = anonymousObject.annotations

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        anonymousObject.replaceAnnotations(newAnnotations)
    }

    override fun replaceAnonymousObject(newAnonymousObject: FirAnonymousObject) {
        anonymousObject = newAnonymousObject
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override val elementKind: FirElementKind
        get() = FirElementKind.AnonymousObjectExpression
}
