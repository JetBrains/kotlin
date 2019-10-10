/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirArraySetCall : FirQualifiedAccess, FirCall {
    override val psi: PsiElement?
    override val annotations: List<FirAnnotationCall>
    override val safe: Boolean
    override val explicitReceiver: FirExpression?
    override val dispatchReceiver: FirExpression
    override val extensionReceiver: FirExpression
    override val calleeReference: FirReference
    override val arguments: List<FirExpression>
    val rValue: FirExpression
    val operation: FirOperation
    val lValue: FirReference
    val indexes: List<FirExpression>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitArraySetCall(this, data)

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirArraySetCall

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirArraySetCall

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirArraySetCall

    fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirArraySetCall

    fun <D> transformIndexes(transformer: FirTransformer<D>, data: D): FirArraySetCall
}
