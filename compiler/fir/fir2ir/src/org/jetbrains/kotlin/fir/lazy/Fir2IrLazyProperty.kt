/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrConst
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.symbols.Fir2IrPropertySymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyProperty(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    fir: FirProperty,
    symbol: Fir2IrPropertySymbol,
    override val isFakeOverride: Boolean
) : AbstractFir2IrLazyDeclaration<FirProperty, IrProperty>(
    components, startOffset, endOffset, origin, fir, symbol
), IrProperty {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
        typeParameters = emptyList()
    }

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = super.descriptor as PropertyDescriptor

    override val symbol: Fir2IrPropertySymbol
        get() = super.symbol as Fir2IrPropertySymbol

    override val isVar: Boolean
        get() = fir.isVar

    override val isConst: Boolean
        get() = fir.isConst

    override val isLateinit: Boolean
        get() = fir.isLateInit

    override val isDelegated: Boolean
        get() = fir.delegate != null

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isExpect: Boolean
        get() = fir.isExpect

    override val name: Name
        get() = fir.name

    override var visibility: Visibility
        get() = fir.visibility
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val modality: Modality
        get() = fir.modality!!

    private val type: IrType by lazy {
        with(typeConverter) { fir.returnTypeRef.toIrType() }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override var backingField: IrField? by lazyVar {
        // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
        val parent = parent
        when {
            !fir.isConst && (fir.modality == Modality.ABSTRACT || parent is IrClass && parent.isInterface) -> {
                null
            }
            fir.initializer != null || fir.getter is FirDefaultPropertyGetter || fir.isVar && fir.setter is FirDefaultPropertySetter -> {
                with(declarationStorage) {
                    createBackingField(
                        fir, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, descriptor,
                        fir.fieldVisibility, fir.name, fir.isVal, fir.initializer,
                        type
                    ).also { field ->
                        val initializer = fir.initializer
                        if (initializer is FirConstExpression<*>) {
                            // TODO: Normally we shouldn't have error type here
                            val constType = with(typeConverter) { initializer.typeRef.toIrType().takeIf { it !is IrErrorType } ?: type }
                            field.initializer = factory.createExpressionBody(initializer.toIrConst(constType))
                        }
                    }
                }
            }
            fir.delegate != null -> {
                with(declarationStorage) {
                    createBackingField(
                        fir, IrDeclarationOrigin.PROPERTY_DELEGATE, descriptor,
                        fir.fieldVisibility, Name.identifier("${fir.name}\$delegate"), true, fir.delegate
                    )
                }
            }
            else -> {
                null
            }
        }?.apply {
            this.parent = this@Fir2IrLazyProperty.parent
        }
    }

    override var getter: IrSimpleFunction? by lazyVar {
        declarationStorage.createIrPropertyAccessor(
            fir.getter, fir, this, type, parent, parent as? IrClass, false,
            when {
                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                fir.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            startOffset, endOffset
        )
    }

    override var setter: IrSimpleFunction? by lazyVar {
        if (!fir.isVar) return@lazyVar null
        declarationStorage.createIrPropertyAccessor(
            fir.setter, fir, this, type, parent, parent as? IrClass, true,
            when {
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                fir.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            startOffset, endOffset
        )
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        backingField = backingField?.transform(transformer, data) as? IrField
        getter = getter?.run { transform(transformer, data) as IrSimpleFunction }
        setter = setter?.run { transform(transformer, data) as IrSimpleFunction }
    }
}
