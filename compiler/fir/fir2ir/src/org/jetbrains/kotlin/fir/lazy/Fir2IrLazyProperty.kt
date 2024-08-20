/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isAnnotationClass
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.PropertySymbols
import org.jetbrains.kotlin.fir.backend.lazyMappedPropertyListVar
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isFacadeClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class Fir2IrLazyProperty(
    private val c: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val fir: FirProperty,
    val containingClass: FirRegularClass?,
    symbols: PropertySymbols,
    parent: IrDeclarationParent,
    override var isFakeOverride: Boolean,
) : IrProperty(), AbstractFir2IrLazyDeclaration<FirProperty>, Fir2IrComponents by c {
    override val symbol: IrPropertySymbol = symbols.propertySymbol

    override var startOffset: Int = startOffset
        set(_) = shouldNotBeCalled()
    override var endOffset: Int = endOffset
        set(_) = shouldNotBeCalled()

    init {
        this.parent = parent
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

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

    override var visibility: DescriptorVisibility = c.visibilityConverter.convertToDescriptorVisibility(fir.visibility)
        set(_) = mutationNotSupported()

    override var modality: Modality
        get() = fir.modality!!
        set(_) = mutationNotSupported()

    private val type: IrType by lazy {
        fir.returnTypeRef.toIrType(c)
    }

    private fun toIrInitializer(initializer: FirExpression?): IrExpressionBody? {
        // Annotations need full initializer information to instantiate them correctly
        return when {
            containingClass?.classKind?.isAnnotationClass == true -> {
                var irInitializer: IrExpressionBody? = null
                declarationStorage.withScope(symbol) {
                    with(declarationStorage) {
                        val firPrimaryConstructor = fir.containingClassLookupTag()
                            ?.toRegularClassSymbol(session)
                            ?.fir
                            ?.primaryConstructorIfAny(session)
                            ?: return@with

                        @OptIn(UnsafeDuringIrConstructionAPI::class)
                        declarationStorage.getIrConstructorSymbol(firPrimaryConstructor).owner.putParametersInScope(firPrimaryConstructor.fir)
                    }
                    fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    irInitializer = initializer?.asCompileTimeIrInitializer(c, fir.returnTypeRef.coneType)
                }
                irInitializer
            }
            // Setting initializers to every other class causes some cryptic errors in lowerings
            initializer is FirLiteralExpression -> {
                fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                val constType = fir.initializer?.resolvedType?.toIrType(c) ?: initializer.resolvedType.toIrType(c)
                factory.createExpressionBody(initializer.toIrConst(constType))
            }
            else -> null
        }
    }

    override var backingField: IrField? = when {
        symbols.backingFieldSymbol == null -> null
        fir.hasExplicitBackingField -> {
            val backingFieldType = fir.backingField?.returnTypeRef?.toIrType(c)
            val evaluatedInitializer = fir.evaluatedInitializer?.unwrapOr<FirExpression> {}
            val initializer = fir.backingField?.initializer ?: evaluatedInitializer ?: fir.initializer
            val visibility = fir.backingField?.visibility ?: fir.visibility
            callablesGenerator.createBackingField(
                this@Fir2IrLazyProperty,
                fir,
                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                symbols.backingFieldSymbol,
                c.visibilityConverter.convertToDescriptorVisibility(visibility),
                fir.name,
                fir.isVal,
                initializer,
                backingFieldType
            ).also { field ->
                field.initializer = toIrInitializer(initializer)
            }
        }
        fir.delegate != null -> {
            fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            callablesGenerator.createBackingField(
                this@Fir2IrLazyProperty,
                fir,
                IrDeclarationOrigin.PROPERTY_DELEGATE,
                symbols.backingFieldSymbol,
                c.visibilityConverter.convertToDescriptorVisibility(fir.visibility),
                NameUtils.propertyDelegateName(fir.name),
                true,
                fir.delegate
            )
        }
        origin != IrDeclarationOrigin.FAKE_OVERRIDE -> {
            fir.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            callablesGenerator.createBackingField(
                this@Fir2IrLazyProperty,
                fir,
                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                symbols.backingFieldSymbol,
                c.visibilityConverter.convertToDescriptorVisibility(fir.visibility),
                fir.name,
                fir.isVal,
                fir.initializer,
                type
            ).also { field ->
                val evaluatedInitializer = fir.evaluatedInitializer?.unwrapOr<FirExpression> {}
                field.initializer = toIrInitializer(evaluatedInitializer ?: fir.initializer)
            }
        }
        else -> null
    }?.apply {
        this.parent = this@Fir2IrLazyProperty.parent
        this.annotations = fir.backingField?.annotations?.mapNotNull {
            callGenerator.convertToIrConstructorCall(it) as? IrConstructorCall
        }.orEmpty()
    }

    override var getter: IrSimpleFunction? = symbols.getterSymbol?.let {
        Fir2IrLazyPropertyAccessor(
            c, startOffset, endOffset,
            origin = when {
                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                fir.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            firAccessor = fir.getter,
            isSetter = false,
            firParentProperty = fir,
            firParentClass = containingClass,
            symbol = it,
            parent = this@Fir2IrLazyProperty.parent,
            isFakeOverride = isFakeOverride,
            correspondingPropertySymbol = this.symbol
        ).also {
            initializeAccessor(it, fir.getter, ConversionTypeOrigin.DEFAULT)
        }
    }

    override var setter: IrSimpleFunction? = run {
        if (!fir.isVar || symbols.setterSymbol == null) return@run null
        Fir2IrLazyPropertyAccessor(
            c, startOffset, endOffset,
            origin = when {
                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                fir.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                fir.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                else -> origin
            },
            firAccessor = fir.setter, isSetter = true,
            firParentProperty = fir,
            firParentClass = containingClass,
            symbol = symbols.setterSymbol,
            parent = this@Fir2IrLazyProperty.parent,
            isFakeOverride = isFakeOverride,
            correspondingPropertySymbol = this.symbol
        ).also {
            initializeAccessor(it, fir.setter, ConversionTypeOrigin.SETTER)
        }
    }

    private fun initializeAccessor(accessor: Fir2IrLazyPropertyAccessor, firAccessor: FirPropertyAccessor?, typeOrigin: ConversionTypeOrigin) {
        declarationStorage.enterScope(accessor.symbol)

        accessor.classifiersGenerator.setTypeParameters(accessor, fir, typeOrigin)

        val containingClass = (parent as? IrClass)?.takeUnless { it.isFacadeClass }
        if (containingClass != null && accessor.shouldHaveDispatchReceiver(containingClass)) {
            accessor.dispatchReceiverParameter = accessor.declareThisReceiverParameter(
                c,
                thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"),
                thisOrigin = accessor.origin,
            )
        }

        accessor.extensionReceiverParameter = fir.receiverParameter?.let {
            accessor.declareThisReceiverParameter(
                c,
                thisType = it.typeRef.toIrType(typeConverter, typeOrigin),
                thisOrigin = accessor.origin,
                explicitReceiver = it
            )
        }

        accessor.valueParameters = buildList {
            callablesGenerator.addContextReceiverParametersTo(
                accessor.fir.contextReceiversForFunctionOrContainingProperty(),
                accessor,
                this@buildList
            )

            if (accessor.isSetter) {
                val valueParameter = firAccessor?.valueParameters?.firstOrNull()
                add(
                    callablesGenerator.createDefaultSetterParameter(
                        accessor.startOffset, accessor.endOffset,
                        (valueParameter?.returnTypeRef ?: accessor.fir.returnTypeRef).toIrType(
                            typeConverter, typeOrigin
                        ),
                        parent = accessor,
                        firValueParameter = valueParameter,
                        name = valueParameter?.name?.takeUnless { firAccessor is FirDefaultPropertySetter },
                        isCrossinline = valueParameter?.isCrossinline == true,
                        isNoinline = valueParameter?.isNoinline == true
                    )
                )
            }
        }

        declarationStorage.leaveScope(accessor.symbol)
    }

    override var overriddenSymbols: List<IrPropertySymbol> by symbolsMappingForLazyClasses.lazyMappedPropertyListVar(lock) lazy@{
        if (containingClass == null || parent !is Fir2IrLazyClass) return@lazy emptyList()

        val baseFunctionWithDispatchReceiverTag =
            lazyFakeOverrideGenerator.computeFakeOverrideKeys(containingClass, fir.symbol)
        baseFunctionWithDispatchReceiverTag.map { (symbol, dispatchReceiverLookupTag) ->
            declarationStorage.getIrPropertySymbol(symbol, dispatchReceiverLookupTag) as IrPropertySymbol
        }
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}
