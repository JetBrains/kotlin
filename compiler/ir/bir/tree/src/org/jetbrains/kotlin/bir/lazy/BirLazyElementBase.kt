/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.lazy

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class BirLazyElementBase(
    internal val converter: Ir2BirConverter,
) : BirElementBase(), BirDeclaration {
    protected abstract val originalElement: IrDeclaration

    final override val parent: BirElementBase? by lazyVar<BirLazyElementBase, _> {
        converter.remapElement(originalElement.parent)
    }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        _parent = new
    }

    internal fun initChild(new: BirElementBase?) {
        if (new != null) {
            new._parent = this
            root?.elementAttached(new)
        }
    }

    protected fun<Bir : BirElementBase> convertChild(originalChild: IrElement): Bir {
        val new = converter.remapElement<Bir>(originalChild)
        initChild(new)
        return new
    }

    @JvmName("convertChildNullable")
    protected fun<Bir : BirElementBase> convertChild(originalChild: IrElement?): Bir? = if(originalChild == null) null else convertChild(originalChild)

    protected fun <P : BirLazyElementBase, T> lazyVar(initializer: P.() -> T): SynchronizedLazyBirElementVar<P, T> =
        SynchronizedLazyBirElementVar(this, initializer)

    @Suppress("UNCHECKED_CAST")
    protected fun <P : BirLazyElementBase, E : BirElement?> lazyChildElementList(
        id: Int,
        retrieveUpstreamList: P.() -> List<IrElement>,
    ) = BirLazyChildElementList<E>(this, id, false, retrieveUpstreamList as BirLazyElementBase.() -> List<IrElement>)


    override val sourceSpan: SourceSpan
        get() = SourceSpan(originalElement.startOffset, originalElement.endOffset)

    override var signature: IdSignature?
        get() = originalElement.symbol.signature
        set(value) = mutationNotSupported()


    override var origin: IrDeclarationOrigin
        get() = originalElement.origin
        set(value) = mutationNotSupported()


    final override fun replaceWith(new: BirElement?) = mutationNotSupported()

    companion object {
        fun mutationNotSupported(): Nothing =
            error("Mutation of lazy BIR elements is not possible")
    }
}
