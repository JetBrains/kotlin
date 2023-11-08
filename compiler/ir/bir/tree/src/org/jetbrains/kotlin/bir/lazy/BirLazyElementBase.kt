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
import kotlin.concurrent.Volatile

abstract class BirLazyElementBase(
    internal val converter: Ir2BirConverter,
) : BirElementBase(), BirDeclaration {
    internal abstract val originalIrElement: IrDeclaration

    @Volatile
    private var parentInitialized = false

    final override val parent: BirElementBase
        get() {
            if (parentInitialized) return _parent as BirElementBase
            synchronized(this) {
                if (!parentInitialized) {
                    _parent = converter.remapElement<BirElementBase>(originalIrElement.parent)
                    parentInitialized = true
                }

                return _parent as BirElementBase
            }
        }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        require(!parentInitialized)
        _parent = new!!
        if (new is BirElementBase) {
            parentInitialized = true
        }
    }

    internal fun initChild(new: BirElementBase?) {
        if (new != null) {
            new._parent = this
            (new as? BirLazyElementBase)?.parentInitialized = true
            root?.elementAttached(new)
        }
    }

    protected fun <Bir : BirElementBase> convertChild(originalChild: IrElement): Bir {
        val new = converter.remapElement<Bir>(originalChild)
        initChild(new)
        return new
    }

    @JvmName("convertChildNullable")
    protected fun <Bir : BirElementBase> convertChild(originalChild: IrElement?): Bir? =
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
