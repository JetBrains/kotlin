/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker

/**
 * Usage of marked API can be unsafe at the stage when IR for the whole module is not built yet (specifically in fir2ir)
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class UnsafeDuringIrConstructionAPI

/**
 * A special object that can be used to refer to [IrDeclaration]s and some other entities from IR nodes.
 *
 * For example, [IrCall] uses [IrSimpleFunctionSymbol] to refer to the [IrSimpleFunction] that is being called.
 *
 * **Q:** Why not just use the [IrSimpleFunction] class itself?
 *
 * **A:** Because a symbol, unlike a declaration, can be bound or unbound (see [isBound]).
 *
 * We need this distinction to work with IR before the linkage phase. In pre-linkage IR the symbols referencing declarations from
 * other modules are not yet bound.
 *
 * During the linkage phase, we collect all the unbound symbols in the IR tree and try to resolve them to the declarations they should
 * refer to. For unbound symbols for public declarations from other modules, [signature] is used to resolve those declarations.
 *
 * @see IdSignature
 * @see SymbolTable
 */
interface IrSymbol : DeclarationSymbolMarker {

    /**
     * The declaration that this symbol refers to if it's bound.
     *
     * If the symbol is unbound, throws [IllegalStateException].
     *
     * **Q:** Why we didn't make this property nullable instead of throwing an exception?
     *
     * **A:** Because we most often need to access a symbol's owner in lowerings, which happen after linkage, at which point all symbols
     * should be already bound. Declaring this property nullable would make working with it more difficult most of the time.
     */
    @UnsafeDuringIrConstructionAPI
    val owner: IrSymbolOwner

    /**
     * If [hasDescriptor] is `true`, returns the [DeclarationDescriptor] of the declaration that this symbol was created for.
     * Otherwise, returns a dummy [IrBasedDeclarationDescriptor] that serves as a descriptor-like view to [owner].
     */
    @ObsoleteDescriptorBasedAPI
    val descriptor: DeclarationDescriptor

    /**
     * Returns `true` if this symbol was created from a [DeclarationDescriptor] either emitted by the K1 (aka classic) frontend,
     * or from deserialized metadata.
     *
     * @see descriptor
     */
    @ObsoleteDescriptorBasedAPI
    val hasDescriptor: Boolean

    /**
     * Whether this symbol has already been resolved to its [owner].
     */
    val isBound: Boolean

    /**
     * If this symbol refers to a publicly accessible declaration (from the binary artifact point of view),
     * returns the binary signature of that declaration.
     *
     * Otherwise, returns `null`.
     *
     * @see IdSignature.isPubliclyVisible
     */
    val signature: IdSignature?

    // TODO: remove once JS IR IC migrates to a different stable tag generation scheme
    // Used to store signatures in private symbols for JS IC
    /**
     * If this symbol refers to a local declaration, the signature of that declaration, otherwise `null`.
     *
     * @see IdSignature.isPubliclyVisible
     */
    var privateSignature: IdSignature?
}

/**
 * Whether this symbol refers to a publicly accessible declaration (from the binary artifact point of view).
 *
 * The symbol doesn't have to be bound.
 */
val IrSymbol.isPublicApi: Boolean
    get() = signature != null

/**
 * A stricter-typed [IrSymbol] that allows to set the owner using the [bind] method. The owner can be set only once.
 *
 * In fact, any [IrSymbol] is [IrBindableSymbol], but having a non-generic interface like [IrSymbol] is sometimes useful.
 *
 * Only leaf interfaces in the symbol hierarchy inherit from this interface.
 */
interface IrBindableSymbol<out Descriptor : DeclarationDescriptor, Owner : IrSymbolOwner> : IrSymbol {
    @UnsafeDuringIrConstructionAPI
    override val owner: Owner

    @ObsoleteDescriptorBasedAPI
    override val descriptor: Descriptor

    /**
     * Sets this symbol's owner.
     *
     * Throws [IllegalStateException] if this symbol has already been bound.
     */
    fun bind(owner: Owner)
}
