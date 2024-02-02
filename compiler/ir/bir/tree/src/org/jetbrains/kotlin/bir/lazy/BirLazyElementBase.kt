/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.lazy

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.BirDeclaration
import org.jetbrains.kotlin.bir.declarations.BirSymbolOwner
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class BirLazyElementBase(
    internal val converter: Ir2BirConverter,
) : BirElementBase(), BirDeclaration, BirSymbolOwner, BirSymbol {
    internal abstract val originalIrElement: IrDeclaration

    final override val isBound: Boolean
        get() = true
    /*override val owner: BirSymbolOwner
        get() = this*/

    internal fun initParent(parent: BirElementParent?) {
        assert(_parent == null) { "Parent of lazy IR element changed" }
        _parent = parent!!
    }

    internal fun initChild(new: BirElementBase?) {
        // Do not set the child's parent to this element, like it is usually done.
        // Instead, Ir2BirConverter should setup this child element upon its creation,
        // including seting its parent and containingDatabase.
        // In particular, it will set the parent element to the same as the
        // original lazy IR element, even though it may be (indefinitely!) incorrect.
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


    override var sourceSpan: SourceSpan
        get() {
            with(converter) {
                return SourceSpan(originalIrElement.startOffset, originalIrElement.endOffset)
            }
        }
        set(value) = mutationNotSupported()

    override var signature: IdSignature?
        get() = originalIrElement.symbol.signature
        set(value) = mutationNotSupported()

    override var origin: IrDeclarationOrigin
        get() = originalIrElement.origin
        set(value) = mutationNotSupported()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    final override fun <T> getDynamicProperty(token: BirDynamicPropertyAccessToken<*, T>): T? {
        if (token.key == GlobalBirDynamicProperties.Descriptor) {
            @Suppress("UNCHECKED_CAST")
            return originalIrElement.descriptor as T
        }

        return super.getDynamicProperty(token)
    }


    final override fun replaceWith(new: BirElement?) = mutationNotSupported()

    companion object {
        fun mutationNotSupported(): Nothing =
            error("Mutation of lazy BIR elements is not possible")
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {

    }
}
