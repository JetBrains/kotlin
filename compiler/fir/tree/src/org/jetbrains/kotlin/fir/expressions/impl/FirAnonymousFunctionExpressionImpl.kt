/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transform

internal class FirAnonymousFunctionExpressionImpl(
    override val source: KtSourceElement?,
    override var anonymousFunction: FirAnonymousFunction
) : FirAnonymousFunctionExpression() {
    override val typeRef: FirTypeRef get() = anonymousFunction.typeRef
    override val annotations: List<FirAnnotation>
        get() = anonymousFunction.annotations

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        anonymousFunction.replaceTypeRef(newTypeRef) // TODO WUT
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        anonymousFunction.replaceAnnotations(newAnnotations)
    }

    override fun replaceAnonymousFunction(newAnonymousFunction: FirAnonymousFunction) {
        anonymousFunction = newAnonymousFunction
    }

    override val elementKind: FirElementKind
        get() = FirElementKind.AnonymousFunctionExpression
}
