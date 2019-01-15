/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirDelegatedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirDelegatedTypeImpl(
    override var type: FirType,
    override var delegate: FirExpression?
) : FirAbstractElement(type.session, type.psi), FirDelegatedType {
    override val annotations: List<FirAnnotationCall>
        get() = type.annotations

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        type = type.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)

        return this
    }
}