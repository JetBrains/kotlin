/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.generators.generateOverriddenPropertySymbols
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.Fir2IrPropertySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyProperty(
    private val components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirProperty,
    val containingClass: FirRegularClass?,
    override val symbol: Fir2IrPropertySymbol,
    override var isFakeOverride: Boolean
) : IrProperty(), AbstractFir2IrLazyDeclaration<FirProperty>, Fir2IrComponents by components {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir, symbol)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()
    override lateinit var parent: IrDeclarationParent

    @ObsoleteDescriptorBasedAPI
    override val descriptor: PropertyDescriptor
        get() = symbol.descriptor

    override var isVar: Boolean
        get() = fir.isVar
        set(_) = mutationNotSupported()

    override var isConst: Boolean
        get() = fir.isConst
        set(_) = mutationNotSupported()

    override var isLateinit: Boolean
        get() = fir.isLateInit
        set(_) = mutationNotSupported()

    override var isDelegated: Boolean
        get() = fir.delegate != null
        set(_) = mutationNotSupported()

    override var isExternal: Boolean
        get() = fir.isExternal
        set(_) = mutationNotSupported()

    override var isExpect: Boolean
        get() = fir.isExpect
        set(_) = mutationNotSupported()

    override var name: Name
        get() = fir.name
        set(_) = mutationNotSupported()

    override var visibility: DescriptorVisibility = components.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var modality: Modality
        get() = fir.modality!!
        set(_) = mutationNotSupported()

    private val type: IrType by lazy {
        with(typeConverter) { fir.returnTypeRef.toIrType() }
    }

    private fun toIrInitializer(initializer: FirExpression?): IrExpressionBody? {
        // Annotations need full initializer information to instantiate them correctly
        return when {
            containingClass?.classKind?.isAnnotationClass == true -> initializer?.asCompileTimeIrInitializer(components)
            // Setting initializers to every other class causes some cryptic errors in lowerings
            initializer is FirConstExpression<*> -> {
                val constType = with(typeConverter) { initializer.resolvedType.toIrType() }
                factory.createExpressionBody(initializer.toIrConst(constType))
            }
            else -> null
        }
    }

    override var backingField: IrField? by lazyVar(lock) {
        when {
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
            fir.hasBackingField && origin != IrDeclarationOrigin.FAKE_OVERRIDE -> {
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
                        NameUtils.propertyDelegateName(fir.name), true, fir.delegate
                    )
                }
            }
            else -> {
                null
            }
        }?.apply {
            this.parent = this@Fir2IrLazyProperty.parent
            this.annotations = fir.backingField?.annotations?.mapNotNull {
                callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
            }.orEmpty()
        }
    }

    override var getter: IrSimpleFunction? by lazyVar(lock) {
        val signature = signatureComposer.composeAccessorSignature(
            fir,
            isSetter = false,
            containingClass?.symbol?.toLookupTag()
        )!!
        symbolTable.declareSimpleFunction(signature, symbolFactory = { Fir2IrSimpleFunctionSymbol(signature) }) { symbol ->
            Fir2IrLazyPropertyAccessor(
                components, startOffset, endOffset,
                when {
                    origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                    fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                    origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                    origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                    fir.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                    else -> origin
                },
                fir.getter, isSetter = false, fir, containingClass, symbol, isFakeOverride
            )
        }.apply {
            parent = this@Fir2IrLazyProperty.parent
            correspondingPropertySymbol = this@Fir2IrLazyProperty.symbol
            with(classifierStorage) {
                setTypeParameters(
                    this@Fir2IrLazyProperty.fir, ConversionTypeOrigin.DEFAULT
                )
            }
        }
    }

    override var setter: IrSimpleFunction? by lazyVar(lock) {
        if (!fir.isVar) null
        else {
            val signature = signatureComposer.composeAccessorSignature(
                fir,
                isSetter = true,
                containingClass?.symbol?.toLookupTag()
            )!!
            symbolTable.declareSimpleFunction(signature, symbolFactory = { Fir2IrSimpleFunctionSymbol(signature) }) { symbol ->
                Fir2IrLazyPropertyAccessor(
                    components, startOffset, endOffset,
                    when {
                        origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                        fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                        origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                        origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                        fir.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                        else -> origin
                    },
                    fir.setter, isSetter = true, fir, containingClass, symbol, isFakeOverride
                ).apply {
                    parent = this@Fir2IrLazyProperty.parent
                    correspondingPropertySymbol = this@Fir2IrLazyProperty.symbol
                    with(classifierStorage) {
                        setTypeParameters(
                            this@Fir2IrLazyProperty.fir, ConversionTypeOrigin.SETTER
                        )
                    }
                }
            }
        }
    }

    override var overriddenSymbols: List<IrPropertySymbol> by lazyVar(lock) {
        if (containingClass == null) return@lazyVar emptyList()
        fir.generateOverriddenPropertySymbols(containingClass)
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}
