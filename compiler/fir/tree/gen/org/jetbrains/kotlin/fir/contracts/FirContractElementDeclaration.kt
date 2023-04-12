/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionElement
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirContractElementDeclaration : FirPureAbstractElement(), FirElement {
    abstract override val source: KtSourceElement?
    abstract val effect: ConeContractDescriptionElement

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitContractElementDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformContractElementDeclaration(this, data) as E
}
