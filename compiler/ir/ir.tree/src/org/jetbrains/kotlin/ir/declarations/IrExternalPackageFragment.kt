/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrExternalPackageFragment : IrPackageFragment() {
    abstract override val symbol: IrExternalPackageFragmentSymbol
    abstract val containerSource: DeserializedContainerSource?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragment(this, data)

    override fun <R, D> accept(visitor: IrThinVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragment(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> acceptChildren(visitor: IrThinVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildren(consumer: IrElementConsumer) {
        declarations.acceptEach(consumer)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
    }
}
