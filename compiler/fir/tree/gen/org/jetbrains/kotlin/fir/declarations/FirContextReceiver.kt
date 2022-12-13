/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirContextReceiver : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val typeRef: FirTypeRef
    abstract val customLabelName: Name?
    abstract val labelNameFromTypeRef: Name?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitContextReceiver(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformContextReceiver(this, data) as E

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun <D> transformTypeRef(transformer: FirTransformer<D>, data: D): FirContextReceiver
}
