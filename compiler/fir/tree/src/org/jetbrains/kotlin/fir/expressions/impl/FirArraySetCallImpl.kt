/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirArraySetCallImpl(
    psi: PsiElement?,
    override var rValue: FirExpression,
    override val operation: FirOperation
) : FirArraySetCall(psi), FirModifiableQualifiedAccess<FirReference> {
    override lateinit var calleeReference: FirReference

    override var lValue: FirReference
        get() = calleeReference
        set(value) {
            calleeReference = value
        }

    override var explicitReceiver: FirExpression?
        get() = null
        set(_) {}

    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override val indexes = mutableListOf<FirExpression>()

    override fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirAssignment {
        rValue = rValue.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirCall {
        rValue = rValue.transformSingle(transformer, data)
        indexes.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        calleeReference = calleeReference.transformSingle(transformer, data)
        rValue = rValue.transformSingle(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        indexes.transformInplace(transformer, data)

        return super<FirArraySetCall>.transformChildren(transformer, data)
    }
}