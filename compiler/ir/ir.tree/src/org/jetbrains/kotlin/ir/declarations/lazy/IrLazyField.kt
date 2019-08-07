/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrLazyField(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override val name: Name,
    override val visibility: Visibility,
    override val isFinal: Boolean,
    override val isExternal: Boolean,
    override val isStatic: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) : IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrField {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrFieldSymbol,
        stubGenerator: DeclarationStubGenerator,
        typeTranslator: TypeTranslator
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        !symbol.descriptor.isVar,
        symbol.descriptor.isEffectivelyExternal(),
        symbol.descriptor.dispatchReceiverParameter == null,
        stubGenerator,
        typeTranslator
    )

    init {
        symbol.bind(this)
    }

    override val annotations: MutableList<IrConstructorCall> = mutableListOf()

    override val descriptor: PropertyDescriptor = symbol.descriptor

    override val overriddenSymbols: MutableList<IrFieldSymbol> = mutableListOf()

    override var type: IrType by lazyVar {
        descriptor.type.toIrType()
    }

    override var initializer: IrExpressionBody? by lazyVar {
        descriptor.compileTimeInitializer?.let {
            IrExpressionBodyImpl(
                typeTranslator.constantValueGenerator.generateConstantValueAsExpression(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it)
            )
        }
    }

    @Suppress("OverridingDeprecatedMember")
    override var correspondingProperty: IrProperty?
        get() = correspondingPropertySymbol?.owner
        set(value) {
            correspondingPropertySymbol = value?.symbol
        }

    override var correspondingPropertySymbol: IrPropertySymbol? by lazyVar {
        stubGenerator.generatePropertyStub(descriptor).symbol
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitField(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }
}
