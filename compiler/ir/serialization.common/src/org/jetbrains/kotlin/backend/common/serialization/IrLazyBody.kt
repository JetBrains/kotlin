/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.withInitialIr
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

sealed class IrLazyBody<B : IrBody>(private val linker: KotlinIrLinker, private var lambda: (() -> B)?) : IrBody {
    override val startOffset: Int
        get() = delegate.startOffset
    override val endOffset: Int
        get() = delegate.endOffset

    private var storage: B? = null

    class RecursiveComputing : Exception()

    protected val delegate: B
        get() {
            if (storage == null) {
                val l = lambda ?:
                    throw RecursiveComputing()
                lambda = null

                val tmp = withInitialIr { l() }
                try {
                    linker.deserializeAllReachableTopLevels()
                    linker.postProcess()
                } catch (ex: RecursiveComputing) {
//                    storage = tmp
//                    linker.postProcess()
                } finally {
                    storage = tmp
                }
            }
            return storage!!
        }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return delegate.accept(visitor, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.acceptChildren(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        delegate.transformChildren(transformer, data)
    }

    class IrLazyExpressionBody(linker: KotlinIrLinker, lambda: () -> IrExpressionBody) : IrLazyBody<IrExpressionBody>(linker, lambda), IrExpressionBody {
        override var expression: IrExpression
            get() = delegate.expression
            set(value) {
                delegate.expression = value
            }
    }

    class IrLazyBlockBody(linker: KotlinIrLinker, lambda: () -> IrBlockBody) : IrLazyBody<IrBlockBody>(linker, lambda), IrBlockBody {
        override val statements: MutableList<IrStatement>
            get() = delegate.statements
    }

    class IrLazySyntheticBody(linker: KotlinIrLinker, lambda: () -> IrSyntheticBody): IrLazyBody<IrSyntheticBody>(linker, lambda), IrSyntheticBody {
        override val kind: IrSyntheticBodyKind
            get() = delegate.kind
    }
}