/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.lazy

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class BirLazyElementBase(
    internal val converter: Ir2BirConverter,
) : BirElementBase(), BirDeclaration {
    internal abstract val originalIrElement: IrDeclaration

    // This is the actual element parent of this element, in case
    // it has one. It may differ from the _parent property in case
    // when we know the parent, but the parent has not (yet)
    // accepted us as one of their children. Then we may still be
    // structurally not bound, or bound directly to a database.
    private var parentElement: BirElementBase? = null

    final override val parent: BirElementBase
        get() {
            synchronized(this) {
                return parentElement ?: converter.remapElement<BirElementBase>(originalIrElement.parent)
                    .also { parentElement = it }
            }
        }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        assert(_parent !is BirElementBase)
        _parent = new!!
        if (new is BirElementBase) {
            parentElement = new
        }
    }

    internal fun initChild(new: BirElementBase?) {
        if (new != null) {
            new._parent = this
            (new as? BirLazyElementBase)?.parentElement = this
            _containingDatabase?.elementAttached(new)
        }
    }

    protected fun <Bir : BirElement> convertChild(originalChild: IrElement): Bir {
        val new = converter.remapElement<Bir>(originalChild)
        initChild(new as BirElementBase?)
        return new
    }

    @JvmName("convertChildNullable")
    protected fun <Bir : BirElement?> convertChild(originalChild: IrElement?): Bir? =
        if (originalChild == null) null else convertChild(originalChild)

    protected fun <P : BirLazyElementBase, T> lazyVar(initializer: P.() -> T): SynchronizedLazyBirElementVar<P, T> =
        SynchronizedLazyBirElementVar(this, initializer)

    @Suppress("UNCHECKED_CAST")
    protected fun <P : BirLazyElementBase, E : BirElement?> lazyChildElementList(
        id: Int,
        retrieveUpstreamList: P.() -> List<IrElement>,
    ) = BirLazyChildElementList<E>(this, id, false, retrieveUpstreamList as BirLazyElementBase.() -> List<IrElement>)


    override val sourceSpan: SourceSpan
        get() = SourceSpan(originalIrElement.startOffset, originalIrElement.endOffset)

    override var signature: IdSignature?
        get() = originalIrElement.symbol.signature
        set(value) = mutationNotSupported()


    override var origin: IrDeclarationOrigin
        get() = originalIrElement.origin
        set(value) = mutationNotSupported()


    final override fun replaceWith(new: BirElement?) = mutationNotSupported()

    companion object {
        fun mutationNotSupported(): Nothing =
            error("Mutation of lazy BIR elements is not possible")
    }
}
