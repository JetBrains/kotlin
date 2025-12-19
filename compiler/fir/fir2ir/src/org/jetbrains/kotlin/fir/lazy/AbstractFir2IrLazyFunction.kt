/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.AbstractIrLazyFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.isFacadeClass
import kotlin.properties.ReadWriteProperty

abstract class AbstractFir2IrLazyFunction<F : FirCallableDeclaration>(
    protected val c: Fir2IrComponents,
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    parent: IrDeclarationParent,
    override var isFakeOverride: Boolean,
) : AbstractIrLazyFunction(), AbstractFir2IrLazyDeclaration<F>, Fir2IrTypeParametersContainer, IrLazyFunctionBase,
    Fir2IrComponents by c {
    init {
        this.parent = parent
    }

    override lateinit var typeParameters: List<IrTypeParameter>

    override var isTailrec: Boolean
        get() = fir.isTailRec
        set(_) = mutationNotSupported()

    override var isSuspend: Boolean
        get() = fir.isSuspend
        set(_) = mutationNotSupported()

    override var isOperator: Boolean
        get() = fir.isOperator
        set(_) = mutationNotSupported()

    override var isInfix: Boolean
        get() = fir.isInfix
        set(_) = mutationNotSupported()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    override var isInline: Boolean
        get() = fir.isInline
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = fir.isExpect
        set(_) = mutationNotSupported()

    @Suppress("LeakingThis")
    override var body: IrBody? by lazyVar(lock) {
        if (tryLoadIr()) body else null
    }

    @Suppress("LeakingThis")
    override var visibility: DescriptorVisibility by lazyVar(lock) {
        c.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
    }

    override var modality: Modality
        get() = fir.symbol.resolvedStatus.modality
        set(_) = mutationNotSupported()

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var attributeOwnerId: IrElement = this

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    internal fun shouldHaveDispatchReceiver(containingClass: IrClass): Boolean {
        return !fir.isStatic && !containingClass.isFacadeClass
    }

    override val factory: IrFactory
        get() = super<AbstractFir2IrLazyDeclaration>.factory

    override fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> {
        return super<AbstractFir2IrLazyDeclaration>.createLazyAnnotations()
    }

    override val isDeserializationEnabled: Boolean
        get() = extensions.irNeedsDeserialization
}
