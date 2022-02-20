/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.backend.ConversionTypeContext
import org.jetbrains.kotlin.fir.backend.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.toIrConst
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.symbols.Fir2IrPropertySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol

class Fir2IrLazyProperty(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirProperty,
    internal val containingClass: FirRegularClass,
    override val symbol: Fir2IrPropertySymbol,
    override val isFakeOverride: Boolean
) : IrProperty(), AbstractFir2IrLazyDeclaration<FirProperty, IrProperty>, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override var typeParameters: List<IrTypeParameter> = emptyList()
    override lateinit var parent: IrDeclarationParent

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

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

    override var name: Name
        get() = fir.name
        set(_) {
            throw UnsupportedOperationException()
        }

    @Suppress("SetterBackingFieldAssignment")
    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val modality: Modality
        get() = fir.modality!!

    private val type: IrType by lazy {
        with(typeConverter) { fir.returnTypeRef.toIrType() }
    }

    private fun toIrInitializer(initializer: FirExpression?): IrExpressionBody? {
        return if (initializer is FirConstExpression<*>) {
            // TODO: Normally we shouldn't have error type here
            val constType = with(typeConverter) { initializer.typeRef.toIrType().takeIf { it !is IrErrorType } ?: type }
            factory.createExpressionBody(initializer.toIrConst(constType))
        } else {
            null
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override var backingField: IrField? by lazyVar(lock) {
        // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
        val parent = parent
        when {
            !fir.isConst && (fir.modality == Modality.ABSTRACT || parent is IrClass && parent.isInterface) -> {
                null
            }
            fir.hasExplicitBackingField -> {
                with(declarationStorage) {
                    val backingFieldType = with(typeConverter) {
                        fir.backingField?.returnTypeRef?.toIrType()
                    }
                    val initializer = fir.backingField?.initializer ?: fir.initializer
                    val visibility = fir.backingField?.visibility ?: fir.visibility
                    createBackingField(
                        fir,
                        IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                        components.visibilityConverter.convertToDescriptorVisibility(visibility),
                        fir.name,
                        fir.isVal,
                        initializer,
                        backingFieldType
                    ).also { field ->
                        field.initializer = toIrInitializer(initializer)
                    }
                }
            }
            fir.initializer != null || fir.getter is FirDefaultPropertyGetter || fir.isVar && fir.setter is FirDefaultPropertySetter -> {
                with(declarationStorage) {
                    createBackingField(
                        fir, IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                        components.visibilityConverter.convertToDescriptorVisibility(fir.visibility), fir.name, fir.isVal, fir.initializer,
                        type
                    ).also { field ->
                        field.initializer = toIrInitializer(fir.initializer)
                    }
                }
            }
            fir.delegate != null -> {
                with(declarationStorage) {
                    createBackingField(
                        fir, IrDeclarationOrigin.PROPERTY_DELEGATE,
                        components.visibilityConverter.convertToDescriptorVisibility(fir.visibility),
                        Name.identifier("${fir.name}\$delegate"), true, fir.delegate
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

    override var getter: IrSimpleFunction? by lazyVar(lock) {
        if (fir.isConst) return@lazyVar null
        Fir2IrLazyPropertyAccessor(
            components, startOffset, endOffset,
            when {
                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                fir.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            fir.getter, isSetter = false, fir, containingClass,
            Fir2IrSimpleFunctionSymbol(
                signatureComposer.composeAccessorSignature(fir, isSetter = false, containingClass.symbol.toLookupTag())!!
            ),
            isFakeOverride
        ).apply {
            parent = this@Fir2IrLazyProperty.parent
            correspondingPropertySymbol = this@Fir2IrLazyProperty.symbol
            with(classifierStorage) {
                setTypeParameters(
                    this@Fir2IrLazyProperty.fir, ConversionTypeContext(
                        definitelyNotNull = false,
                        origin = ConversionTypeOrigin.DEFAULT
                    )
                )
            }
        }
    }

    override var setter: IrSimpleFunction? by lazyVar(lock) {
        if (!fir.isVar) null else Fir2IrLazyPropertyAccessor(
            components, startOffset, endOffset,
            when {
                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                fir.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            fir.setter, isSetter = true, fir, containingClass,
            Fir2IrSimpleFunctionSymbol(
                signatureComposer.composeAccessorSignature(fir, isSetter = true, containingClass.symbol.toLookupTag())!!
            ),
            isFakeOverride
        ).apply {
            parent = this@Fir2IrLazyProperty.parent
            correspondingPropertySymbol = this@Fir2IrLazyProperty.symbol
            with(classifierStorage) {
                setTypeParameters(
                    this@Fir2IrLazyProperty.fir, ConversionTypeContext(
                        definitelyNotNull = false,
                        origin = ConversionTypeOrigin.SETTER
                    )
                )
            }
        }
    }

    override var overriddenSymbols: List<IrPropertySymbol> by lazyVar(lock) {
        fir.generateOverriddenPropertySymbols(
            containingClass,
            session,
            scopeSession,
            declarationStorage,
            fakeOverrideGenerator
        )
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    override var attributeOwnerId: IrAttributeContainer = this
}
