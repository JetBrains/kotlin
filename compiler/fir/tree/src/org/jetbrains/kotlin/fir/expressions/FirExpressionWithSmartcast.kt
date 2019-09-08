/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitor

abstract class FirExpressionWithSmartcast(
    open val originalExpression: FirQualifiedAccessExpression,
    val typesFromSmartcast: Collection<ConeKotlinType>
) : FirQualifiedAccessExpression(originalExpression.psi) {
    val originalType: FirTypeRef get() = originalExpression.typeRef

    final override val safe: Boolean
        get() = originalExpression.safe

    final override val explicitReceiver: FirExpression?
        get() = originalExpression.explicitReceiver

    final override val dispatchReceiver: FirExpression
        get() = originalExpression.dispatchReceiver

    final override val extensionReceiver: FirExpression
        get() = originalExpression.extensionReceiver

    final override val calleeReference: FirReference
        get() = originalExpression.calleeReference

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        originalExpression.acceptChildren(visitor, data)
    }
}