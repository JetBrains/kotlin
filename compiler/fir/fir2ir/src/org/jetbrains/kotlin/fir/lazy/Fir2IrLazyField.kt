/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.utils.toIrConst
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyField(
    private val c: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirField,
    val containingClass: FirRegularClass?,
    override val symbol: IrFieldSymbol,
) : IrField(), AbstractFir2IrLazyDeclaration<FirField>, Fir2IrComponents by c {
    init {
        symbol.bind(this)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isFinal: Boolean
        get() = fir.isFinal
        set(_) = mutationNotSupported()

    override var isStatic: Boolean
        get() = fir.isStatic
        set(_) = mutationNotSupported()

    override var name: Name
        get() = fir.name
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility = c.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var type: IrType
        get() = fir.returnTypeRef.toIrType(c)
        set(_) = mutationNotSupported()

    override var initializer: IrExpressionBody? by lazyVar(lock) {
        when (val initializer = fir.unwrapFakeOverrides().initializer) {
            is FirLiteralExpression<*> -> factory.createExpressionBody(initializer.toIrConst(type))
            else -> null
        }
    }

    override var correspondingPropertySymbol: IrPropertySymbol?
        get() = null
        set(_) = mutationNotSupported()

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")
}
