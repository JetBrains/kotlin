/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.isKFunctionInvoke
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrScriptImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.impl.SCRIPT_K2_ORIGIN
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Fir2IrCallableDeclarationsGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {
    // ------------------------------------ package fragments ------------------------------------

    internal fun createExternalPackageFragment(fqName: FqName, moduleDescriptor: FirModuleDescriptor): IrExternalPackageFragment {
        return createExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
    }

    internal fun createExternalPackageFragment(packageFragmentDescriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        val symbol = IrExternalPackageFragmentSymbolImpl(packageFragmentDescriptor)
        return IrExternalPackageFragmentImpl(symbol, packageFragmentDescriptor.fqName)
    }

    // ------------------------------------ functions ------------------------------------

    fun createIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        symbol: IrSimpleFunctionSymbol,
        predefinedOrigin: IrDeclarationOrigin?,
        isLocal: Boolean,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        allowLazyDeclarationsCreation: Boolean
    ): IrSimpleFunction = convertCatching(function) {
        val simpleFunction = function as? FirSimpleFunction
        val isLambda = function is FirAnonymousFunction && function.isLambda
        val updatedOrigin = when {
            isLambda -> IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            function.symbol.callableId.isKFunctionInvoke() -> IrDeclarationOrigin.FAKE_OVERRIDE
            !predefinedOrigin.isExternal && // we should preserve origin for external enums
                    simpleFunction?.isStatic == true &&
                    simpleFunction.name in Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES
            -> IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER

            // Kotlin built-in class and Java originated method (Collection.forEach, etc.)
            // It's necessary to understand that such methods do not belong to DefaultImpls but actually generated as default
            // See org.jetbrains.kotlin.backend.jvm.lower.InheritedDefaultMethodsOnClassesLoweringKt.isDefinitelyNotDefaultImplsMethod
            (irParent as? IrClass)?.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                    function.isJavaOrEnhancement -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            else -> function.computeIrOrigin(
                predefinedOrigin,
                parentOrigin = (irParent as? IrDeclaration)?.origin,
                fakeOverrideOwnerLookupTag
            )
        }
        if (irParent.isExternalParent()) {
            require(function is FirSimpleFunction)
            if (!allowLazyDeclarationsCreation) {
                error("Lazy functions should be processed in Fir2IrDeclarationStorage")
            }
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            if (symbol.isBound) return symbol.owner
            return lazyDeclarationsGenerator.createIrLazyFunction(function, symbol, irParent, updatedOrigin)
        }
        val name = simpleFunction?.name
            ?: if (isLambda) SpecialNames.ANONYMOUS else SpecialNames.NO_NAME_PROVIDED
        val visibility = simpleFunction?.visibility ?: Visibilities.Local
        val isSuspend =
            @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.type?.isSuspendOrKSuspendFunctionType(session) == true
            else function.isSuspend
        val created = function.convertWithOffsets { startOffset, endOffset ->
            classifierStorage.preCacheTypeParameters(function, symbol)
            irFactory.createSimpleFunction(
                startOffset = if (updatedOrigin == IrDeclarationOrigin.DELEGATED_MEMBER) SYNTHETIC_OFFSET else startOffset,
                endOffset = if (updatedOrigin == IrDeclarationOrigin.DELEGATED_MEMBER) SYNTHETIC_OFFSET else endOffset,
                origin = updatedOrigin,
                name = name,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(visibility),
                isInline = simpleFunction?.isInline == true,
                isExpect = simpleFunction?.isExpect == true,
                returnType = function.returnTypeRef.toIrType(),
                modality = simpleFunction?.modality ?: Modality.FINAL,
                symbol = symbol,
                isTailrec = simpleFunction?.isTailRec == true,
                isSuspend = isSuspend,
                isOperator = simpleFunction?.isOperator == true,
                isInfix = simpleFunction?.isInfix == true,
                isExternal = simpleFunction?.isExternal == true,
                containerSource = simpleFunction?.containerSource,
            ).apply {
                metadata = FirMetadataSource.Function(function)
                declarationStorage.withScope(symbol) {
                    /*
                     * `isLocal = true` indicates that a function is local or member of a local class
                     * containingClassLookupTag allows to distinguish those two cases
                     */
                    setParent(irParent)
                    if (!(isLocal && function.containingClassLookupTag() == null)) {
                        addDeclarationToParent(this, irParent)
                    }
                    declareParameters(
                        function, irParent,
                        dispatchReceiverType = computeDispatchReceiverType(this, simpleFunction, irParent),
                        isStatic = simpleFunction?.isStatic == true,
                        forSetter = false,
                    )
                    convertAnnotationsForNonDeclaredMembers(function, origin)
                }
            }
        }

        if (visibility == Visibilities.Local) {
            return created
        }
        if (function.symbol.callableId.isKFunctionInvoke()) {
            (function.symbol.originalForSubstitutionOverride as? FirNamedFunctionSymbol)?.let {
                created.overriddenSymbols += declarationStorage.getIrFunctionSymbol(it) as IrSimpleFunctionSymbol
            }
        }
        return created
    }

    private val IrDeclarationOrigin?.isExternal: Boolean
        get() = (this == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB || this == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB)

    // ------------------------------------ constructors ------------------------------------

    fun createIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        symbol: IrConstructorSymbol,
        predefinedOrigin: IrDeclarationOrigin? = null,
        allowLazyDeclarationsCreation: Boolean
    ): IrConstructor = convertCatching(constructor) {
        val origin = constructor.computeIrOrigin(predefinedOrigin, irParent.origin)
        val isPrimary = constructor.isPrimary
        if (irParent is Fir2IrLazyClass) {
            if (!allowLazyDeclarationsCreation) {
                error("Lazy constructors should be processed in Fir2IrDeclarationStorage")
            }
            val lazyConstructor = lazyDeclarationsGenerator.createIrLazyConstructor(constructor, symbol, origin, irParent)
            lazyConstructor.prepareTypeParameters()
            return lazyConstructor
        }
        val visibility = if (irParent.isAnonymousObject) Visibilities.Public else constructor.visibility
        return constructor.convertWithOffsets { startOffset, endOffset ->
            classifierStorage.preCacheTypeParameters(constructor, symbol)
            irFactory.createConstructor(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = SpecialNames.INIT,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(visibility),
                isInline = false,
                isExpect = constructor.isExpect,
                returnType = constructor.returnTypeRef.toIrType(),
                symbol = symbol,
                isPrimary = isPrimary,
                isExternal = false,
            ).apply {
                metadata = FirMetadataSource.Function(constructor)
                annotationGenerator.generate(this, constructor)
                declarationStorage.withScope(symbol) {
                    setParent(irParent)
                    addDeclarationToParent(this, irParent)
                    declareParameters(constructor, irParent, dispatchReceiverType = null, isStatic = false, forSetter = false)
                }
            }
        }
    }

    // ------------------------------------ properties ------------------------------------

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        symbols: PropertySymbols,
        predefinedOrigin: IrDeclarationOrigin? = null,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        allowLazyDeclarationsCreation: Boolean
    ): IrProperty = convertCatching(property) {
        val origin = when {
            !predefinedOrigin.isExternal &&
                    property.isStatic &&
                    property.name in Fir2IrDeclarationStorage.ENUM_SYNTHETIC_NAMES
            -> IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER

            else -> property.computeIrOrigin(
                predefinedOrigin,
                parentOrigin = (irParent as? IrDeclaration)?.origin,
                fakeOverrideOwnerLookupTag
            )
        }
        // See similar comments in createIrFunction above
        val parentIsExternal = irParent.isExternalParent()
        if (parentIsExternal) {
            if (!allowLazyDeclarationsCreation) {
                error("Lazy properties should be processed in Fir2IrDeclarationStorage")
            }
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            if (symbols.propertySymbol.isBound) return symbols.propertySymbol.owner
            // For private functions signature is null, fallback to non-lazy property
            return lazyDeclarationsGenerator.createIrLazyProperty(property, irParent!!, symbols, origin)
        }
        return property.convertWithOffsets { startOffset, endOffset ->
            classifierStorage.preCacheTypeParameters(property, symbols.propertySymbol)
            irFactory.createProperty(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = property.name,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(property.visibility),
                modality = property.modality!!,
                symbol = symbols.propertySymbol,
                isVar = property.isVar,
                isConst = property.isConst,
                isLateinit = property.isLateInit,
                isDelegated = property.delegate != null,
                isExternal = property.isExternal,
                containerSource = property.containerSource,
                isExpect = property.isExpect,
            ).apply {
                metadata = FirMetadataSource.Property(property)
                convertAnnotationsForNonDeclaredMembers(property, origin)
                declarationStorage.withScope(symbol) {
                    // IrProperty is never created for local variables
                    setParent(irParent)
                    addDeclarationToParent(this, irParent)
                    val type = property.returnTypeRef.toIrType()
                    val delegate = property.delegate
                    val getter = property.getter
                    val setter = property.setter
                    if (delegate != null || property.hasBackingField) {
                        val backingField = if (delegate != null) {
                            ((delegate as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedBaseSymbol()?.fir as? FirTypeParameterRefsOwner)?.let {
                                classifierStorage.preCacheTypeParameters(it, symbol)
                            }
                            createBackingField(
                                this,
                                property,
                                IrDeclarationOrigin.PROPERTY_DELEGATE,
                                symbols.backingFieldSymbol!!,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                NameUtils.propertyDelegateName(property.name),
                                true,
                                delegate
                            )
                        } else {
                            val initializer = getEffectivePropertyInitializer(property, resolveIfNeeded = true)
                            // There are cases when we get here for properties
                            // that have no backing field. For example, in the
                            // funExpression.kt test there's an attempt
                            // to access the `javaClass` property of the `foo0`'s
                            // `block` argument
                            val typeToUse = property.backingField?.returnTypeRef?.toIrType() ?: type
                            createBackingField(
                                this,
                                property,
                                IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                                symbols.backingFieldSymbol!!,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                property.name,
                                property.isVal,
                                initializer,
                                typeToUse
                            ).also { field ->
                                if (initializer is FirLiteralExpression<*>) {
                                    val constType = initializer.resolvedType.toIrType()
                                    field.initializer = factory.createExpressionBody(initializer.toIrConst(constType))
                                }
                            }
                        }
                        this.backingField = backingField
                    }
                    if (irParent != null) {
                        backingField?.parent = irParent
                    }
                    this.getter = getter.convertWithOffsets(startOffset, endOffset) { startOffset, endOffset ->
                        createIrPropertyAccessor(
                            getter, property, this, symbols.getterSymbol, type, irParent, false,
                            when {
                                origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                                origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> origin
                                delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                                origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                                getter == null || getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset,
                            property.unwrapFakeOverrides().getter,
                        )
                    }
                    if (property.isVar) {
                        this.setter = setter.convertWithOffsets(startOffset, endOffset) { startOffset, endOffset ->
                            createIrPropertyAccessor(
                                setter, property, this, symbols.setterSymbol!!, type, irParent, true,
                                when {
                                    delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                    origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                                    origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                                    setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                    else -> origin
                                },
                                startOffset, endOffset,
                                property.unwrapFakeOverrides().setter,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * In partial module compilation (see [org.jetbrains.kotlin.analysis.api.fir.components.KtFirCompilerFacility]),
     * referenced properties might be resolved only up to [FirResolvePhase.CONTRACTS],
     * however the backend requires the exact initializer type.
     */
    private fun getEffectivePropertyInitializer(property: FirProperty, resolveIfNeeded: Boolean): FirExpression? {
        val initializer = property.backingField?.initializer ?: property.initializer

        if (resolveIfNeeded && initializer is FirLiteralExpression<*>) {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return getEffectivePropertyInitializer(property, resolveIfNeeded = false)
        }

        return initializer
    }

    fun generateIrPropertyForSyntheticPropertyReference(
        propertySymbol: FirSimpleSyntheticPropertySymbol,
        parent: IrDeclarationParent,
    ): IrProperty {
        val property = propertySymbol.fir
        return irFactory.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE,
            name = property.name,
            visibility = visibilityConverter.convertToDescriptorVisibility(property.visibility),
            modality = property.modality ?: Modality.FINAL,
            symbol = IrPropertySymbolImpl(),
            isVar = property.isVar,
            isConst = false,
            isLateinit = property.isLateInit,
            isDelegated = property.delegate != null,
            isExternal = property.isExternal,
            isExpect = property.isExpect
        ).also {
            it.parent = parent
        }
    }

    // ------------------------------------ property accessors ------------------------------------

    private fun createIrPropertyAccessor(
        propertyAccessor: FirPropertyAccessor?,
        property: FirProperty,
        correspondingProperty: IrDeclarationWithName,
        symbol: IrSimpleFunctionSymbol,
        propertyType: IrType,
        irParent: IrDeclarationParent?,
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int,
        propertyAccessorForAnnotations: FirPropertyAccessor? = propertyAccessor,
    ): IrSimpleFunction = convertCatching(propertyAccessor ?: property) {
        val prefix = if (isSetter) "set" else "get"
        val containerSource = (correspondingProperty as? IrProperty)?.containerSource
        val accessorReturnType = if (isSetter) irBuiltIns.unitType else propertyType
        val visibility = propertyAccessor?.visibility?.let {
            components.visibilityConverter.convertToDescriptorVisibility(it)
        }
        return irFactory.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = Name.special("<$prefix-${correspondingProperty.name}>"),
            visibility = visibility ?: (correspondingProperty as IrDeclarationWithVisibility).visibility,
            isInline = propertyAccessor?.isInline == true,
            isExpect = false,
            returnType = accessorReturnType,
            modality = (correspondingProperty as? IrOverridableMember)?.modality ?: Modality.FINAL,
            symbol = symbol,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = propertyAccessor?.isExternal == true,
            containerSource = containerSource,
        ).apply {
            correspondingPropertySymbol = (correspondingProperty as? IrProperty)?.symbol
            if (propertyAccessor != null) {
                metadata = FirMetadataSource.Function(propertyAccessor)
                // Note that deserialized annotations are stored in the accessor, not the property.
                convertAnnotationsForNonDeclaredMembers(propertyAccessor, origin)
            }

            if (propertyAccessorForAnnotations != null) {
                convertAnnotationsForNonDeclaredMembers(propertyAccessorForAnnotations, origin)
            }
            classifiersGenerator.setTypeParameters(
                this, property, if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
            )
            val dispatchReceiverType = computeDispatchReceiverType(this, property, irParent)
            // NB: we should enter accessor' scope before declaring its parameters
            // (both setter default and receiver ones, if any)
            declarationStorage.withScope(symbol) {
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeOrigin.SETTER),
                        firValueParameter = null
                    )
                }
                // property accessors does not belong to declarations of class/file, but are referenced via property,
                //   so there is no need to add accessor to list of parents declarations
                setParent(irParent)
                declareParameters(
                    propertyAccessor, irParent, dispatchReceiverType,
                    isStatic = irParent !is IrClass || propertyAccessor?.isStatic == true, forSetter = isSetter,
                    parentPropertyReceiver = property.receiverParameter,
                )
            }
            if (correspondingProperty is Fir2IrLazyProperty && correspondingProperty.containingClass != null && !isFakeOverride && dispatchReceiverType != null) {
                @OptIn(FirBasedFakeOverrideGenerator::class) // for lazy
                this.overriddenSymbols = correspondingProperty.fir.generateOverriddenAccessorSymbols(
                    correspondingProperty.containingClass, !isSetter
                )
            }
        }
    }

    // ------------------------------------ fields ------------------------------------

    internal fun createBackingField(
        irProperty: IrProperty,
        firProperty: FirProperty,
        origin: IrDeclarationOrigin,
        symbol: IrFieldSymbol,
        visibility: DescriptorVisibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField = convertCatching(firProperty) {
        val inferredType = type ?: firInitializerExpression!!.resolvedType.toIrType()
        return (firProperty.delegate ?: firProperty.backingField ?: firProperty).convertWithOffsets { startOffset: Int, endOffset: Int ->
            irFactory.createField(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name,
                visibility = visibility,
                symbol = symbol,
                type = inferredType,
                isFinal = isFinal,
                isStatic = firProperty.isStatic || !(irProperty.parent is IrClass || irProperty.parent is IrScript),
                isExternal = firProperty.isExternal,
            ).also {
                it.correspondingPropertySymbol = irProperty.symbol
            }.apply {
                metadata = FirMetadataSource.Property(firProperty)
                convertAnnotationsForNonDeclaredMembers(firProperty, origin)
            }
        }
    }

    private val FirProperty.fieldVisibility: Visibility
        get() = when {
            hasExplicitBackingField -> backingField?.visibility ?: status.visibility
            isLateInit -> setter?.visibility ?: status.visibility
            isConst -> status.visibility
            hasJvmFieldAnnotation(session) -> status.visibility
            origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> status.visibility
            else -> Visibilities.Private
        }

    internal fun createIrField(
        field: FirField,
        irParent: IrDeclarationParent?,
        symbol: IrFieldSymbol,
        type: ConeKotlinType = field.returnTypeRef.coneType,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField = convertCatching(field) {
        val irType = type.toIrType()

        val parentIsExternal = irParent.isExternalParent()
        if (field is FirJavaField && field.isStatic && field.isFinal && parentIsExternal) {
            // We are going to create IR for Java static final fields lazily because they can refer to some Kotlin const.
            // This way we delay const evaluation of Java fields until IR tree is fully built, and we can run IR interpreter.
            return lazyDeclarationsGenerator.createIrLazyField(field, symbol, irParent!!, origin).apply {
                setParent(irParent)
                addDeclarationToParent(this, irParent)
            }
        }

        return field.convertWithOffsets { startOffset, endOffset ->
            irFactory.createField(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = field.name,
                visibility = components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                symbol = symbol,
                type = irType,
                isFinal = field.modality == Modality.FINAL,
                isStatic = field.isStatic,
                isExternal = false
            ).apply {
                metadata = FirMetadataSource.Field(field)
                val initializer = field.unwrapFakeOverrides().initializer
                if (initializer is FirLiteralExpression<*>) {
                    this.initializer = factory.createExpressionBody(initializer.toIrConst(irType))
                }
                /*
                 * fields of regular properties are stored inside IrProperty
                 * fields for delegates (inheritance by delegation) are stored in the corresponding class directly
                 */
                setParent(irParent)
                if (origin == IrDeclarationOrigin.DELEGATE) {
                    addDeclarationToParent(this, irParent)
                }
            }
        }
    }

    // ------------------------------------ parameters ------------------------------------

    private fun <T : IrFunction> T.declareDefaultSetterParameter(type: IrType, firValueParameter: FirValueParameter?): T {
        valueParameters = listOf(
            createDefaultSetterParameter(startOffset, endOffset, type, parent = this, firValueParameter)
        )
        return this
    }

    internal fun createDefaultSetterParameter(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        parent: IrFunction,
        firValueParameter: FirValueParameter?,
        name: Name? = null,
        isCrossinline: Boolean = false,
        isNoinline: Boolean = false,
    ): IrValueParameter {
        return irFactory.createValueParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.DEFINED,
            name = name ?: SpecialNames.IMPLICIT_SET_PARAMETER,
            type = type,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            index = parent.contextReceiverParametersCount,
            varargElementType = null,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = false,
        ).apply {
            this.parent = parent
            if (firValueParameter != null) {
                annotationGenerator.generate(this, firValueParameter)
            }
        }
    }

    fun addContextReceiverParametersTo(
        contextReceivers: List<FirContextReceiver>,
        parent: IrFunction,
        result: MutableList<IrValueParameter>,
    ) {
        contextReceivers.mapIndexedTo(result) { index, contextReceiver ->
            createIrParameterFromContextReceiver(contextReceiver, index).apply {
                this.parent = parent
            }
        }
    }

    private fun createIrParameterFromContextReceiver(
        contextReceiver: FirContextReceiver,
        index: Int,
    ): IrValueParameter = convertCatching(contextReceiver) {
        val type = contextReceiver.typeRef.toIrType()
        return contextReceiver.convertWithOffsets { startOffset, endOffset ->
            irFactory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = IrDeclarationOrigin.DEFINED,
                name = NameUtils.contextReceiverName(index),
                type = type,
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(),
                index = index,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false,
            )
        }
    }

    /*
     * In perfect world dispatchReceiverType should always be the default type of containing class
     * But fake-overrides for members from Any have special rules for type of dispatch receiver
     */
    private fun IrFunction.declareParameters(
        function: FirFunction?,
        irParent: IrDeclarationParent?,
        dispatchReceiverType: IrType?, // has no sense for constructors
        isStatic: Boolean,
        forSetter: Boolean,
        // Can be not-null only for property accessors
        parentPropertyReceiver: FirReceiverParameter? = null
    ) {
        val containingClass = computeContainingClass(irParent)
        val parent = this
        if (function is FirSimpleFunction || function is FirConstructor) {
            classifiersGenerator.setTypeParameters(this, function)
        }
        val typeOrigin = if (forSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
        if (function is FirDefaultPropertySetter) {
            val valueParameter = function.valueParameters.first()
            val type = valueParameter.returnTypeRef.toIrType(ConversionTypeOrigin.SETTER)
            declareDefaultSetterParameter(type, valueParameter)
        } else if (function != null) {
            val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

            contextReceiverParametersCount = contextReceivers.size
            valueParameters = buildList {
                addContextReceiverParametersTo(contextReceivers, parent, this)

                function.valueParameters.mapIndexedTo(this) { index, valueParameter ->
                    declarationStorage.createAndCacheParameter(
                        valueParameter, index + contextReceiverParametersCount,
                        useStubForDefaultValueStub = function !is FirConstructor || containingClass?.name != Name.identifier("Enum"),
                        typeOrigin,
                        skipDefaultParameter = isFakeOverride || origin == IrDeclarationOrigin.DELEGATED_MEMBER,
                        forcedDefaultValueConversion = containingClass?.isAnnotationClass == true
                    ).apply {
                        this.parent = parent
                    }
                }
            }
        }

        val thisOrigin = IrDeclarationOrigin.DEFINED
        if (function !is FirConstructor) {
            val receiver: FirReceiverParameter? =
                if (function !is FirPropertyAccessor && function != null) function.receiverParameter
                else parentPropertyReceiver
            if (receiver != null) {
                extensionReceiverParameter = receiver.convertWithOffsets { startOffset, endOffset ->
                    val name = (function as? FirAnonymousFunction)?.label?.name?.let {
                        val suffix = it.takeIf(Name::isValidIdentifier) ?: "\$receiver"
                        Name.identifier("\$this\$$suffix")
                    } ?: SpecialNames.THIS
                    declareThisReceiverParameter(
                        thisType = receiver.typeRef.toIrType(typeOrigin),
                        thisOrigin = thisOrigin,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        name = name,
                        explicitReceiver = receiver,
                        isAssignable = function.shouldParametersBeAssignable()
                    )
                }
            }
            // See [LocalDeclarationsLowering]: "local function must not have dispatch receiver."
            val isLocal = function is FirSimpleFunction && function.isLocal
            if (function !is FirAnonymousFunction && dispatchReceiverType != null && !isStatic && !isLocal) {
                dispatchReceiverParameter = declareThisReceiverParameter(
                    thisType = dispatchReceiverType,
                    thisOrigin = thisOrigin
                )
            }
        } else {
            // Set dispatch receiver parameter for inner class's constructor.
            val outerClass = containingClass?.parentClassOrNull
            if (containingClass?.isInner == true && outerClass != null) {
                dispatchReceiverParameter = declareThisReceiverParameter(
                    thisType = outerClass.thisReceiver!!.type,
                    thisOrigin = thisOrigin
                )
            }
        }
    }

    internal fun createIrParameter(
        valueParameter: FirValueParameter,
        index: Int = UNDEFINED_PARAMETER_INDEX,
        useStubForDefaultValueStub: Boolean = true,
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
        skipDefaultParameter: Boolean = false,
        // Use this parameter if you want to insert the actual default value instead of the stub (overrides useStubForDefaultValueStub parameter).
        // This parameter is intended to be used for default values of annotation parameters where they are needed and
        // may produce incorrect results for values that may be encountered outside annotations.
        // Does not do anything if valueParameter.defaultValue is already FirExpressionStub.
        forcedDefaultValueConversion: Boolean = false,
        predefinedOrigin: IrDeclarationOrigin? = null
    ): IrValueParameter = convertCatching(valueParameter) {
        val origin = valueParameter.computeIrOrigin(predefinedOrigin)
        val type = valueParameter.returnTypeRef.toIrType(typeOrigin)
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            irFactory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = valueParameter.name,
                type = type,
                isAssignable = valueParameter.containingFunctionSymbol.fir.shouldParametersBeAssignable(),
                symbol = IrValueParameterSymbolImpl(),
                index = index,
                varargElementType = valueParameter.varargElementType?.toIrType(typeOrigin),
                isCrossinline = valueParameter.isCrossinline,
                isNoinline = valueParameter.isNoinline,
                isHidden = false,
            ).apply {
                val defaultValue = valueParameter.defaultValue
                if (!skipDefaultParameter && defaultValue != null) {
                    this.defaultValue = when {
                        forcedDefaultValueConversion && defaultValue !is FirExpressionStub ->
                            defaultValue.asCompileTimeIrInitializer(components)
                        useStubForDefaultValueStub || defaultValue !is FirExpressionStub ->
                            factory.createExpressionBody(
                                IrErrorExpressionImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, type,
                                    "Stub expression for default value of ${valueParameter.name}"
                                )
                            )
                        else -> null
                    }
                }
                annotationGenerator.generate(this, valueParameter)
            }
        }
        return irParameter
    }

    // ------------------------------------ local delegated properties ------------------------------------

    fun createIrLocalDelegatedProperty(
        property: FirProperty,
        irParent: IrDeclarationParent,
        symbols: LocalDelegatedPropertySymbols
    ): IrLocalDelegatedProperty = convertCatching(property) {
        val type = property.returnTypeRef.toIrType()
        val origin = IrDeclarationOrigin.DEFINED
        val irProperty = property.convertWithOffsets { startOffset, endOffset ->
            irFactory.createLocalDelegatedProperty(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = property.name,
                symbol = symbols.propertySymbol,
                type = type,
                isVar = property.isVar
            )
        }.apply {
            parent = irParent
            metadata = FirMetadataSource.Property(property)
            declarationStorage.withScope(symbol) {
                delegate = declareIrVariable(
                    startOffset, endOffset, IrDeclarationOrigin.PROPERTY_DELEGATE,
                    NameUtils.propertyDelegateName(property.name), property.delegate!!.resolvedType.toIrType(),
                    isVar = false, isConst = false, isLateinit = false
                )
                delegate.parent = irParent
                getter = createIrPropertyAccessor(
                    property.getter, property, this, symbols.getterSymbol, type, irParent, false,
                    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset
                )
                if (property.isVar) {
                    setter = createIrPropertyAccessor(
                        property.setter, property, this, symbols.setterSymbol!!, type, irParent, true,
                        IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset
                    )
                }
                annotationGenerator.generate(this, property)
            }
        }
        return irProperty
    }

    // ------------------------------------ variables ------------------------------------

    // TODO: KT-58686
    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

    fun declareTemporaryVariable(base: IrExpression, nameHint: String? = null): IrVariable {
        return declareIrVariable(
            base.startOffset, base.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            Name.identifier(getNameForTemporary(nameHint)), base.type,
            isVar = false, isConst = false, isLateinit = false
        ).apply {
            initializer = base
        }
    }

    fun createIrVariable(
        variable: FirVariable,
        irParent: IrDeclarationParent,
        givenOrigin: IrDeclarationOrigin?
    ): IrVariable = convertCatching(variable) {
        val type = variable.irTypeForPotentiallyComponentCall()
        // Some temporary variables are produced in RawFirBuilder, but we consistently use special names for them.
        val origin = when {
            givenOrigin != null -> givenOrigin
            variable.name == SpecialNames.ITERATOR -> IrDeclarationOrigin.FOR_LOOP_ITERATOR
            variable.name.isSpecial -> IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            else -> IrDeclarationOrigin.DEFINED
        }
        val isLateInit = if (variable is FirProperty) variable.isLateInit else false
        val irVariable = variable.convertWithOffsets { startOffset, endOffset ->
            declareIrVariable(
                startOffset, endOffset, origin,
                variable.name, type, variable.isVar, isConst = false, isLateinit = isLateInit
            )
        }
        irVariable.parent = irParent
        return irVariable
    }

    private fun declareIrVariable(
        startOffset: Int, endOffset: Int,
        origin: IrDeclarationOrigin, name: Name, type: IrType,
        isVar: Boolean, isConst: Boolean, isLateinit: Boolean
    ): IrVariable {
        return IrVariableImpl(
            startOffset, endOffset, origin, IrVariableSymbolImpl(), name, type,
            isVar, isConst, isLateinit
        )
    }

    // ------------------------------------ anonymous initializers ------------------------------------

    fun createIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        irParent: IrClass
    ): IrAnonymousInitializer = convertCatching(anonymousInitializer) {
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            irFactory.createAnonymousInitializer(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED,
                IrAnonymousInitializerSymbolImpl()
            ).apply {
                setParent(irParent)
                addDeclarationToParent(this, irParent)
            }
        }
    }

    // ------------------------------------ scripts ------------------------------------

    fun createIrScript(script: FirScript, symbol: IrScriptSymbol): IrScript = script.convertWithOffsets { startOffset, endOffset ->
        IrScriptImpl(symbol, script.name, irFactory, startOffset, endOffset).also { irScript ->
            irScript.origin = SCRIPT_K2_ORIGIN
            irScript.metadata = FirMetadataSource.Script(script)
            irScript.implicitReceiversParameters = emptyList()
            irScript.providedProperties = emptyList()
            irScript.providedPropertiesParameters = emptyList()
        }
    }

    // ------------------------------------ utilities ------------------------------------

    companion object {
        /**
         * [firCallable] is function or property (if [irFunction] is a property accessor) for
         *   which [irFunction] was build
         *
         * It is needed to determine proper dispatch receiver type if this declaration is fake-override
         */
        context(Fir2IrComponents)
        internal fun computeDispatchReceiverType(
            irFunction: IrSimpleFunction,
            firCallable: FirCallableDeclaration?,
            parent: IrDeclarationParent?,
        ): IrType? {
            /*
             * If some function is not fake-override, then its type should be just
             *   default type of containing class
             * For fake overrides the default type calculated in the following way:
             * 1. Find first overridden function, which is not fake override
             * 2. Take its containing class
             * 3. Find supertype of current containing class with type constructor of
             *    class from step 2
             */
            if (firCallable is FirProperty && firCallable.isLocal) return null
            val containingClass = computeContainingClass(parent) ?: return null
            val defaultType = containingClass.defaultType
            if (firCallable == null) return defaultType
            if (!irFunction.isFakeOverride) return defaultType

            val originalCallable = firCallable.unwrapFakeOverrides()
            val containerOfOriginalCallable = originalCallable.containingClassLookupTag() ?: return defaultType
            val containerOfFakeOverride = firCallable.dispatchReceiverType ?: return defaultType
            val correspondingSupertype = AbstractTypeChecker.findCorrespondingSupertypes(
                session.typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
                containerOfFakeOverride,
                containerOfOriginalCallable
            ).firstOrNull() as ConeKotlinType? ?: return defaultType
            return correspondingSupertype.toIrType()
        }

        private fun computeContainingClass(parent: IrDeclarationParent?): IrClass? {
            return if (parent is IrClass && parent !is Fir2IrDeclarationStorage.NonCachedSourceFileFacadeClass) {
                parent
            } else {
                null
            }
        }
    }

    private fun IrMutableAnnotationContainer.convertAnnotationsForNonDeclaredMembers(
        firAnnotationContainer: FirAnnotationContainer, origin: IrDeclarationOrigin,
    ) {
        if ((firAnnotationContainer as? FirDeclaration)?.let { it.isFromLibrary || it.isPrecompiled } == true
            || origin == IrDeclarationOrigin.FAKE_OVERRIDE
        ) {
            annotationGenerator.generate(this, firAnnotationContainer)
        }
    }

    private inline fun <R> convertCatching(element: FirElement, block: () -> R): R {
        try {
            return block()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            errorWithAttachment("Exception was thrown during transformation of ${element::class.java}", cause = e) {
                withFirEntry("element", element)
            }
        }
    }
}

internal fun IrDeclaration.setParent(irParent: IrDeclarationParent?) {
    if (irParent != null) {
        parent = irParent
    }
}

/**
 * We should not try to add declaration to list of parents declarations in two cases:
 * 1. getters, setters, and backing fields are not stored in parent directly. They are stored in IrProperty instead,
 *      which is stored in parent
 * 2. if a declaration is declared in a local scope (in some body) then it will have contained class/function as a parent. But the declaration
 *      should be listed in statements list of the corresponding IrBlock instead of IrClass.declarations
 *      Note that IrClass will be a parent if some declaration is declared inside anonymous initializer, because IrAnonymousInitializer
 *      is not a IrDeclarationParent
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun addDeclarationToParent(declaration: IrDeclaration, irParent: IrDeclarationParent?) {
    if (irParent == null) return
    when (irParent) {
        is Fir2IrLazyClass -> {
            /*
             * Declaration list of lazy class is lazy by itself, and it will collect and store all required members
             * automatically on the first access to Fir2IrLazyClass.declarations
             */
        }
        is IrClass -> irParent.declarations += declaration
        is IrFile -> irParent.declarations += declaration
        is IrExternalPackageFragment -> irParent.declarations += declaration
        is IrScript -> {
            /*
             * All declarations of the script will be added during main script conversion
             */
        }
        else -> error("Can't add declaration ${declaration.render()} to parent ${irParent.render()}")
    }
}

@OptIn(ExperimentalContracts::class)
internal fun IrDeclarationParent?.isExternalParent(): Boolean {
    contract {
        returns(true) implies (this@isExternalParent != null)
    }
    return this is Fir2IrLazyClass || this is IrExternalPackageFragment
}

context(Fir2IrComponents)
internal fun FirCallableDeclaration?.shouldParametersBeAssignable(): Boolean {
    return extensions.parametersAreAssignable && this?.isTailRec == true
}
