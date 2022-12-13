/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.resolve.calls.CallableReferenceMappedArguments
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirResolvedCallableReference : FirResolvedNamedReference() {
    abstract override val source: KtSourceElement?
    abstract override val name: Name
    abstract override val resolvedSymbol: FirBasedSymbol<*>
    abstract val inferredTypeArguments: List<ConeKotlinType>
    abstract val mappedArguments: CallableReferenceMappedArguments

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitResolvedCallableReference(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformResolvedCallableReference(this, data) as E
}
