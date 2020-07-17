/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.backend.generators.AnnotationGenerator
import org.jetbrains.kotlin.fir.backend.generators.DelegatedMemberGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@OptIn(ObsoleteDescriptorBasedAPI::class)
class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor
) : Fir2IrComponents by components {

    internal var annotationGenerator: AnnotationGenerator? = null

    private val firSymbolProvider = session.firSymbolProvider

    private val firProvider = session.firProvider

    private val fragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val builtInsFragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val fileCache = mutableMapOf<FirFile, IrFile>()

    private val functionCache = mutableMapOf<FirFunction<*>, IrSimpleFunction>()

    private val constructorCache = mutableMapOf<FirConstructor, IrConstructor>()

    private val initializerCache = mutableMapOf<FirAnonymousInitializer, IrAnonymousInitializer>()

    private val propertyCache = mutableMapOf<FirProperty, IrProperty>()

    private val fieldCache = mutableMapOf<FirField, IrField>()

    private val localStorage = Fir2IrLocalStorage()

    private val delegatedMemberGenerator = DelegatedMemberGenerator(components)

    private fun areCompatible(firFunction: FirFunction<*>, irFunction: IrFunction): Boolean {
        if (firFunction is FirSimpleFunction && irFunction is IrSimpleFunction) {
            if (irFunction.name != firFunction.name) return false
        }
        return irFunction.valueParameters.size == firFunction.valueParameters.size &&
                irFunction.valueParameters.zip(firFunction.valueParameters).all { (irParameter, firParameter) ->
                    val irType = irParameter.type
                    val firType = firParameter.returnTypeRef.coneType
                    if (irType is IrSimpleType) {
                        when (val irClassifierSymbol = irType.classifier) {
                            is IrTypeParameterSymbol -> {
                                firType is ConeTypeParameterType
                            }
                            is IrClassSymbol -> {
                                val irClass = irClassifierSymbol.owner
                                firType is ConeClassLikeType && irClass.name == firType.lookupTag.name
                            }
                            else -> {
                                false
                            }
                        }
                    } else {
                        false
                    }
                }
    }

    internal fun preCacheBuiltinClassMembers(firClass: FirRegularClass, irClass: IrClass) {
        for (declaration in firClass.declarations) {
            when (declaration) {
                is FirProperty -> {
                    val irProperty = irClass.properties.find { it.name == declaration.name }
                    if (irProperty != null) {
                        propertyCache[declaration] = irProperty
                    }
                }
                is FirSimpleFunction -> {
                    val irFunction = irClass.functions.find {
                        areCompatible(declaration, it)
                    }
                    if (irFunction != null) {
                        functionCache[declaration] = irFunction
                    }
                }
                is FirConstructor -> {
                    val irConstructor = irClass.constructors.find {
                        areCompatible(declaration, it)
                    }
                    if (irConstructor != null) {
                        constructorCache[declaration] = irConstructor
                    }
                }
            }
        }
    }

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

    fun enterScope(declaration: IrDeclaration) {
        symbolTable.enterScope(declaration)
        if (declaration is IrSimpleFunction ||
            declaration is IrConstructor ||
            declaration is IrProperty ||
            declaration is IrEnumEntry
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(declaration: IrDeclaration) {
        if (declaration is IrSimpleFunction ||
            declaration is IrConstructor ||
            declaration is IrProperty ||
            declaration is IrEnumEntry
        ) {
            localStorage.leaveCallable()
        }
        symbolTable.leaveScope(declaration)
    }

    private fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun ConeKotlinType.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun getIrExternalOrBuiltInsPackageFragment(fqName: FqName): IrExternalPackageFragment {
        val isBuiltIn = fqName in BUILT_INS_PACKAGE_FQ_NAMES
        return if (isBuiltIn) getIrBuiltInsPackageFragment(fqName) else getIrExternalPackageFragment(fqName)
    }

    private fun getIrBuiltInsPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return builtInsFragmentCache.getOrPut(fqName) {
            return symbolTable.declareExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
        }
    }

    private fun getIrExternalPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return fragmentCache.getOrPut(fqName) {
            return symbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
        }
    }

    internal fun findIrParent(packageFqName: FqName, parentClassId: ClassId?, firBasedSymbol: FirBasedSymbol<*>): IrDeclarationParent? {
        return if (parentClassId != null) {
            // TODO: this will never work for local classes
            val parentFirSymbol = firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
            if (parentFirSymbol is FirClassSymbol) {
                val parentIrSymbol = classifierStorage.getIrClassSymbol(parentFirSymbol)
                parentIrSymbol.owner
            } else {
                null
            }
        } else {
            val containerFile = when (firBasedSymbol) {
                is FirCallableSymbol -> firProvider.getFirCallableContainerFile(firBasedSymbol)
                is FirClassLikeSymbol -> firProvider.getFirClassifierContainerFileIfAny(firBasedSymbol)
                else -> error("Unknown symbol: $firBasedSymbol")
            }

            when {
                containerFile != null -> fileCache[containerFile]
                firBasedSymbol is FirCallableSymbol -> getIrExternalPackageFragment(packageFqName)
                // TODO: All classes from BUILT_INS_PACKAGE_FQ_NAMES are considered built-ins now,
                // which is not exact and can lead to some problems
                else -> getIrExternalOrBuiltInsPackageFragment(packageFqName)
            }
        }
    }

    internal fun findIrParent(callableDeclaration: FirCallableDeclaration<*>): IrDeclarationParent? {
        val firBasedSymbol = callableDeclaration.symbol
        val callableId = firBasedSymbol.callableId
        return findIrParent(callableId.packageName, callableId.classId, firBasedSymbol)
    }

    private fun IrDeclaration.setAndModifyParent(irParent: IrDeclarationParent?) {
        if (irParent != null) {
            parent = irParent
            if (irParent is IrExternalPackageFragment) {
                irParent.declarations += this
            } else if (irParent is IrClass) {
                // TODO: irParent.declarations += this (probably needed for external stuff)
            }
        }
    }

    private fun <T : IrFunction> T.declareDefaultSetterParameter(type: IrType): T {
        val parent = this
        val descriptor = WrappedValueParameterDescriptor()
        valueParameters = listOf(
            symbolTable.declareValueParameter(
                startOffset, endOffset, origin, descriptor, type
            ) { symbol ->
                IrValueParameterImpl(
                    startOffset, endOffset, IrDeclarationOrigin.DEFINED, symbol,
                    Name.special("<set-?>"), 0, type,
                    varargElementType = null,
                    isCrossinline = false, isNoinline = false
                ).apply {
                    this.parent = parent
                    descriptor.bind(this)
                }
            }
        )
        return this
    }

    private fun <T : IrFunction> T.declareParameters(
        function: FirFunction<*>?,
        containingClass: IrClass?,
        isStatic: Boolean,
        // Can be not-null only for property accessors
        parentPropertyReceiverType: FirTypeRef?
    ) {
        val parent = this
        if (function is FirSimpleFunction || function is FirConstructor) {
            with(classifierStorage) {
                setTypeParameters(function)
            }
        }
        val forSetter = function is FirPropertyAccessor && function.isSetter
        val typeContext = ConversionTypeContext(
            definitelyNotNull = false,
            origin = if (forSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
        )
        if (function is FirDefaultPropertySetter) {
            val type = function.valueParameters.first().returnTypeRef.toIrType(ConversionTypeContext.DEFAULT.inSetter())
            declareDefaultSetterParameter(type)
        } else if (function != null) {
            valueParameters = function.valueParameters.mapIndexed { index, valueParameter ->
                createIrParameter(
                    valueParameter, index,
                    useStubForDefaultValueStub = function !is FirConstructor || containingClass?.name != Name.identifier("Enum"),
                    typeContext
                ).apply {
                    this.parent = parent
                }
            }
        }
        with(classifierStorage) {
            val thisOrigin = IrDeclarationOrigin.DEFINED
            if (function !is FirConstructor) {
                val receiverTypeRef = if (function !is FirPropertyAccessor) function?.receiverTypeRef else parentPropertyReceiverType
                if (receiverTypeRef != null) {
                    extensionReceiverParameter = receiverTypeRef.convertWithOffsets { startOffset, endOffset ->
                        declareThisReceiverParameter(
                            symbolTable,
                            thisType = receiverTypeRef.toIrType(typeContext),
                            thisOrigin = thisOrigin,
                            startOffset = startOffset,
                            endOffset = endOffset
                        )
                    }
                }
                if (function !is FirAnonymousFunction && containingClass != null && !isStatic) {
                    dispatchReceiverParameter = declareThisReceiverParameter(
                        symbolTable,
                        thisType = containingClass.thisReceiver?.type ?: error("No this receiver"),
                        thisOrigin = thisOrigin
                    )
                }
            } else {
                // Set dispatch receiver parameter for inner class's constructor.
                val outerClass = containingClass?.parentClassOrNull
                if (containingClass?.isInner == true && outerClass != null) {
                    dispatchReceiverParameter = declareThisReceiverParameter(
                        symbolTable,
                        thisType = outerClass.thisReceiver!!.type,
                        thisOrigin = thisOrigin
                    )
                }
            }
        }
    }

    private fun <T : IrFunction> T.bindAndDeclareParameters(
        function: FirFunction<*>?,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        isStatic: Boolean,
        parentPropertyReceiverType: FirTypeRef? = null
    ): T {
        if (irParent != null) {
            parent = irParent
        }
        declareParameters(function, thisReceiverOwner, isStatic, parentPropertyReceiverType)
        return this
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction<*>): T {
        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters)) {
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    fun getCachedIrFunction(function: FirFunction<*>): IrSimpleFunction? {
        return if (function !is FirSimpleFunction || function.visibility == Visibilities.LOCAL) {
            localStorage.getLocalFunction(function)
        } else {
            functionCache[function]
        }
    }

    internal fun declareIrSimpleFunction(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        if (signature == null) {
            val descriptor = WrappedSimpleFunctionDescriptor()
            return symbolTable.declareSimpleFunction(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)
    }

    fun createIrFunction(
        function: FirFunction<*>,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrSimpleFunction {
        val simpleFunction = function as? FirSimpleFunction
        val isLambda = function.source?.elementType == KtNodeTypes.FUNCTION_LITERAL
        val updatedOrigin = when {
            isLambda -> IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            function.symbol.callableId.isKFunctionInvoke() -> IrDeclarationOrigin.FAKE_OVERRIDE
            simpleFunction?.isStatic == true && simpleFunction.name in ENUM_SYNTHETIC_NAMES -> IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER
            else -> origin
        }
        classifierStorage.preCacheTypeParameters(function)
        val name = simpleFunction?.name
            ?: if (isLambda) Name.special("<anonymous>") else Name.special("<no name provided>")
        val visibility = simpleFunction?.visibility ?: Visibilities.LOCAL
        val isSuspend =
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.isSuspend == true
            else simpleFunction?.isSuspend == true
        val signature = signatureComposer.composeSignature(function)
        val created = function.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrSimpleFunction(signature, simpleFunction?.containerSource) { symbol ->
                IrFunctionImpl(
                    startOffset, endOffset, updatedOrigin, symbol,
                    name, visibility,
                    simpleFunction?.modality ?: Modality.FINAL,
                    function.returnTypeRef.toIrType(),
                    isInline = simpleFunction?.isInline == true,
                    isExternal = simpleFunction?.isExternal == true,
                    isTailrec = simpleFunction?.isTailRec == true,
                    isSuspend = isSuspend,
                    isExpect = simpleFunction?.isExpect == true,
                    isFakeOverride = updatedOrigin == IrDeclarationOrigin.FAKE_OVERRIDE,
                    isOperator = simpleFunction?.isOperator == true
                ).apply {
                    metadata = FirMetadataSource.Function(function)
                    convertAnnotationsFromLibrary(function)
                    enterScope(this)
                    bindAndDeclareParameters(
                        function, irParent,
                        thisReceiverOwner, isStatic = simpleFunction?.isStatic == true
                    )
                    leaveScope(this)
                }
            }
            result
        }

        if (visibility == Visibilities.LOCAL) {
            localStorage.putLocalFunction(function, created)
            return created
        }
        if (function.symbol.callableId.isKFunctionInvoke()) {
            (function.symbol.overriddenSymbol as? FirNamedFunctionSymbol)?.let {
                created.overriddenSymbols += getIrFunctionSymbol(it) as IrSimpleFunctionSymbol
            }
        }
        functionCache[function] = created
        return created
    }

    fun getCachedIrAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): IrAnonymousInitializer? =
        initializerCache[anonymousInitializer]

    fun createIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        irParent: IrClass
    ): IrAnonymousInitializer {
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareAnonymousInitializer(startOffset, endOffset, IrDeclarationOrigin.DEFINED, irParent.descriptor).apply {
                this.parent = irParent
                initializerCache[anonymousInitializer] = this
            }
        }
    }

    fun getCachedIrConstructor(constructor: FirConstructor): IrConstructor? = constructorCache[constructor]

    private fun declareIrConstructor(signature: IdSignature?, factory: (IrConstructorSymbol) -> IrConstructor): IrConstructor {
        if (signature == null) {
            val descriptor = WrappedClassConstructorDescriptor()
            return symbolTable.declareConstructor(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareConstructor(signature, { Fir2IrConstructorSymbol(signature) }, factory)
    }

    fun createIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrConstructor {
        val isPrimary = constructor.isPrimary
        classifierStorage.preCacheTypeParameters(constructor)
        val signature = signatureComposer.composeSignature(constructor)
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            declareIrConstructor(signature) { symbol ->
                IrConstructorImpl(
                    startOffset, endOffset, origin, symbol,
                    Name.special("<init>"), constructor.visibility,
                    constructor.returnTypeRef.toIrType(),
                    isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = constructor.isExpect
                ).apply {
                    metadata = FirMetadataSource.Function(constructor)
                    enterScope(this)
                    bindAndDeclareParameters(constructor, irParent, isStatic = false)
                    leaveScope(this)
                }
            }
        }
        constructorCache[constructor] = created
        return created
    }

    private fun declareIrAccessor(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        isGetter: Boolean,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        if (signature == null) {
            val descriptor =
                if (isGetter) WrappedPropertyGetterDescriptor(Annotations.EMPTY, SourceElement.NO_SOURCE)
                else WrappedPropertySetterDescriptor(Annotations.EMPTY, SourceElement.NO_SOURCE)
            return symbolTable.declareSimpleFunction(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)
    }

    internal fun createIrPropertyAccessor(
        propertyAccessor: FirPropertyAccessor?,
        property: FirProperty,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int
    ): IrSimpleFunction {
        val prefix = if (isSetter) "set" else "get"
        val signature = signatureComposer.composeAccessorSignature(property, isSetter)
        return declareIrAccessor(
            signature,
            (correspondingProperty.descriptor as? WrappedPropertyDescriptorWithContainerSource)?.containerSource,
            isGetter = !isSetter
        ) { symbol ->
            val accessorReturnType = if (isSetter) irBuiltIns.unitType else propertyType
            IrFunctionImpl(
                startOffset, endOffset, origin, symbol,
                Name.special("<$prefix-${correspondingProperty.name}>"),
                propertyAccessor?.visibility ?: correspondingProperty.visibility,
                correspondingProperty.modality, accessorReturnType,
                isInline = propertyAccessor?.isInline == true,
                isExternal = propertyAccessor?.isExternal == true,
                isTailrec = false, isSuspend = false, isExpect = false,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                isOperator = false
            ).apply {
                correspondingPropertySymbol = correspondingProperty.symbol
                if (propertyAccessor != null) {
                    metadata = FirMetadataSource.Function(propertyAccessor)
                    // Note that deserialized annotations are stored in the accessor, not the property.
                    convertAnnotationsFromLibrary(propertyAccessor)
                }
                with(classifierStorage) {
                    setTypeParameters(
                        property, ConversionTypeContext(
                            definitelyNotNull = false,
                            origin = if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
                        )
                    )
                }
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeContext.DEFAULT.inSetter())
                    )
                }
                enterScope(this)
                bindAndDeclareParameters(
                    propertyAccessor, irParent,
                    thisReceiverOwner, isStatic = irParent !is IrClass, parentPropertyReceiverType = property.receiverTypeRef
                )
                leaveScope(this)
                if (irParent != null) {
                    parent = irParent
                }
                if (!isFakeOverride && thisReceiverOwner != null) {
                    populateOverriddenSymbols(thisReceiverOwner)
                }
            }
        }
    }

    internal fun IrProperty.createBackingField(
        property: FirProperty,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        visibility: Visibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField {
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType()
        return symbolTable.declareField(
            startOffset, endOffset, origin, descriptor, inferredType
        ) { symbol ->
            IrFieldImpl(
                startOffset, endOffset, origin, symbol,
                name, inferredType,
                visibility, isFinal = isFinal,
                isExternal = property.isExternal,
                isStatic = property.isStatic || parent !is IrClass,
            ).also {
                it.correspondingPropertySymbol = this@createBackingField.symbol
            }.apply {
                metadata = FirMetadataSource.Property(property)
                convertAnnotationsFromLibrary(property)
            }
        }
    }

    internal val FirProperty.fieldVisibility: Visibility
        get() = when {
            isLateInit -> setter?.visibility ?: status.visibility
            isConst -> status.visibility
            hasJvmFieldAnnotation -> status.visibility
            else -> Visibilities.PRIVATE
        }

    private fun declareIrProperty(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrPropertySymbol) -> IrProperty
    ): IrProperty {
        if (signature == null) {
            val descriptor = WrappedPropertyDescriptor()
            return symbolTable.declareProperty(0, 0, IrDeclarationOrigin.DEFINED, descriptor, isDelegated = false, factory).apply {
                descriptor.bind(this)
            }
        }
        return symbolTable.declareProperty(signature, { Fir2IrPropertySymbol(signature, containerSource) }, factory)
    }

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrProperty {
        classifierStorage.preCacheTypeParameters(property)
        val signature = signatureComposer.composeSignature(property)
        return property.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrProperty(signature, property.containerSource) { symbol ->
                IrPropertyImpl(
                    startOffset, endOffset, origin, symbol,
                    property.name, property.visibility, property.modality!!,
                    isVar = property.isVar,
                    isConst = property.isConst,
                    isLateinit = property.isLateInit,
                    isDelegated = property.delegate != null,
                    isExternal = property.isExternal,
                    isExpect = property.isExpect,
                    isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
                ).apply {
                    metadata = FirMetadataSource.Variable(property)
                    convertAnnotationsFromLibrary(property)
                    enterScope(this)
                    if (irParent != null) {
                        parent = irParent
                    }
                    val type = property.returnTypeRef.toIrType()
                    val initializer = property.initializer
                    val delegate = property.delegate
                    val getter = property.getter
                    val setter = property.setter
                    // TODO: this checks are very preliminary, FIR resolve should determine backing field presence itself
                    if (property.isConst || (property.modality != Modality.ABSTRACT && (irParent !is IrClass || !irParent.isInterface))) {
                        if (initializer != null || getter is FirDefaultPropertyGetter ||
                            property.isVar && setter is FirDefaultPropertySetter
                        ) {
                            backingField = createBackingField(
                                property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, descriptor,
                                property.fieldVisibility, property.name, property.isVal, initializer,
                                type
                            ).also { field ->
                                if (initializer is FirConstExpression<*>) {
                                    // TODO: Normally we shouldn't have error type here
                                    val constType = initializer.typeRef.toIrType().takeIf { it !is IrErrorType } ?: type
                                    field.initializer = IrExpressionBodyImpl(initializer.toIrConst(constType))
                                }
                            }
                        } else if (delegate != null) {
                            backingField = createBackingField(
                                property, IrDeclarationOrigin.PROPERTY_DELEGATE, descriptor,
                                property.fieldVisibility, Name.identifier("${property.name}\$delegate"), true, delegate
                            )
                        }
                        if (irParent != null) {
                            backingField?.parent = irParent
                        }

                    }
                    this.getter = createIrPropertyAccessor(
                        getter, property, this, type, irParent, thisReceiverOwner, false,
                        when {
                            origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                            delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                            getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                            else -> origin
                        },
                        startOffset, endOffset
                    )
                    if (property.isVar) {
                        this.setter = createIrPropertyAccessor(
                            setter, property, this, type, irParent, thisReceiverOwner, true,
                            when {
                                delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset
                        )
                    }
                    leaveScope(this)
                }
            }
            propertyCache[property] = result
            result
        }
    }

    fun getCachedIrProperty(property: FirProperty): IrProperty? = propertyCache[property]

    fun getCachedIrField(field: FirField): IrField? = fieldCache[field]

    fun createIrFieldAndDelegatedMembers(field: FirField, parent: IrClass): IrField {
        val irField = createIrField(field, origin = IrDeclarationOrigin.DELEGATE)
        irField.setAndModifyParent(parent)
        delegatedMemberGenerator.generate(irField, parent)
        return irField
    }

    internal fun findOverriddenFirFunction(irFunction: IrSimpleFunction, superClassId: ClassId): FirFunction<*>? {
        val functions = getFirClassByFqName(superClassId)?.declarations?.filter {
            it is FirFunction<*> && functionCache.containsKey(it) && irFunction.overrides(functionCache[it]!!)
        }
        return if (functions.isNullOrEmpty()) null else functions.first() as FirFunction<*>
    }

    internal fun findOverriddenFirProperty(irProperty: IrProperty, superClassId: ClassId): FirProperty? {
        val properties = getFirClassByFqName(superClassId)?.declarations?.filter {
            it is FirProperty && it.name == irProperty.name
        }
        return if (properties.isNullOrEmpty()) null else properties.first() as FirProperty
    }

    private fun getFirClassByFqName(classId: ClassId): FirClass<*>? {
        val declaration = firProvider.getClassLikeSymbolByFqName(classId) ?: firSymbolProvider.getClassLikeSymbolByFqName(classId)
        return declaration?.fir as? FirClass<*>
    }

    private fun createIrField(
        field: FirField,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField {
        val descriptor = WrappedFieldDescriptor()
        val type = field.returnTypeRef.toIrType()
        return field.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareField(
                startOffset, endOffset,
                origin, descriptor, type
            ) { symbol ->
                IrFieldImpl(
                    startOffset, endOffset, origin, symbol,
                    field.name, type, field.visibility,
                    isFinal = field.modality == Modality.FINAL,
                    isExternal = false,
                    isStatic = field.isStatic
                ).apply {
                    metadata = FirMetadataSource.Variable(field)
                    descriptor.bind(this)
                    fieldCache[field] = this
                }
            }
        }
    }

    internal fun createIrParameter(
        valueParameter: FirValueParameter,
        index: Int = -1,
        useStubForDefaultValueStub: Boolean = true,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val type = valueParameter.returnTypeRef.toIrType()
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareValueParameter(
                startOffset, endOffset, origin, descriptor, type
            ) { symbol ->
                IrValueParameterImpl(
                    startOffset, endOffset, origin, symbol,
                    valueParameter.name, index, type,
                    if (!valueParameter.isVararg) null
                    else valueParameter.returnTypeRef.coneType.arrayElementType()?.toIrType(typeContext),
                    valueParameter.isCrossinline, valueParameter.isNoinline
                ).apply {
                    descriptor.bind(this)
                    if (valueParameter.defaultValue.let {
                            it != null && (useStubForDefaultValueStub || it !is FirExpressionStub)
                        }
                    ) {
                        this.defaultValue = IrExpressionBodyImpl(
                            IrErrorExpressionImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET, type,
                                "Stub expression for default value of ${valueParameter.name}"
                            )
                        )
                    }
                }
            }
        }
        localStorage.putParameter(valueParameter, irParameter)
        return irParameter
    }

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

    private fun declareIrVariable(
        startOffset: Int, endOffset: Int,
        origin: IrDeclarationOrigin, name: Name, type: IrType,
        isVar: Boolean, isConst: Boolean, isLateinit: Boolean
    ): IrVariable {
        val descriptor = WrappedVariableDescriptor()
        return symbolTable.declareVariable(startOffset, endOffset, origin, descriptor, type) { symbol ->
            IrVariableImpl(
                startOffset, endOffset, origin, symbol, name, type,
                isVar, isConst, isLateinit
            ).apply {
                descriptor.bind(this)
            }
        }
    }

    fun createIrVariable(variable: FirVariable<*>, irParent: IrDeclarationParent, givenOrigin: IrDeclarationOrigin? = null): IrVariable {
        val type = variable.returnTypeRef.toIrType()
        // Some temporary variables are produced in RawFirBuilder, but we consistently use special names for them.
        val origin = when {
            givenOrigin != null -> givenOrigin
            variable.name == Name.special("<iterator>") -> IrDeclarationOrigin.FOR_LOOP_ITERATOR
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
        localStorage.putVariable(variable, irVariable)
        return irVariable
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

    fun getIrConstructorSymbol(firConstructorSymbol: FirConstructorSymbol): IrConstructorSymbol {
        val firConstructor = firConstructorSymbol.fir
        getCachedIrConstructor(firConstructor)?.let { return it.symbol }
        val signature = signatureComposer.composeSignature(firConstructor)
        val irParent = findIrParent(firConstructor) as IrClass
        val parentOrigin = irParent.origin
        if (signature != null) {
            symbolTable.referenceConstructorIfAny(signature)?.let { irConstructorSymbol ->
                val irFunction = irConstructorSymbol.owner
                constructorCache[firConstructor] = irFunction
                return irConstructorSymbol
            }
            assert(parentOrigin != IrDeclarationOrigin.DEFINED) {
                "Should not have reference to public API uncached constructor from source code"
            }
            val symbol = Fir2IrConstructorSymbol(signature)
            val irConstructor = firConstructor.convertWithOffsets { startOffset, endOffset ->
                symbolTable.declareConstructor(signature, { symbol }) {
                    Fir2IrLazyConstructor(
                        components, startOffset, endOffset, parentOrigin, firConstructor, symbol
                    ).apply {
                        parent = irParent
                    }
                }
            }
            constructorCache[firConstructor] = irConstructor
            // NB: this is needed to prevent recursions in case of self bounds
            (irConstructor as Fir2IrLazyConstructor).prepareTypeParameters()
            return symbol
        }
        val irDeclaration = createIrConstructor(firConstructor, irParent, origin = parentOrigin).apply {
            setAndModifyParent(irParent)
        }
        return irDeclaration.symbol
    }

    fun getIrFunctionSymbol(firFunctionSymbol: FirFunctionSymbol<*>): IrFunctionSymbol {
        return when (val firDeclaration = firFunctionSymbol.fir) {
            is FirSimpleFunction, is FirAnonymousFunction -> {
                getCachedIrFunction(firDeclaration)?.let { return it.symbol }
                val signature = signatureComposer.composeSignature(firDeclaration)
                val irParent = findIrParent(firDeclaration)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                if (signature != null) {
                    symbolTable.referenceSimpleFunctionIfAny(signature)?.let { irFunctionSymbol ->
                        val irFunction = irFunctionSymbol.owner
                        functionCache[firDeclaration] = irFunction
                        return irFunctionSymbol
                    }
                    // TODO: package fragment members (?)
                    if (firDeclaration is FirSimpleFunction && irParent is Fir2IrLazyClass) {
                        assert(parentOrigin != IrDeclarationOrigin.DEFINED) {
                            "Should not have reference to public API uncached simple function from source code"
                        }
                        val symbol = Fir2IrSimpleFunctionSymbol(signature, firDeclaration.containerSource)
                        val irFunction = firDeclaration.convertWithOffsets { startOffset, endOffset ->
                            symbolTable.declareSimpleFunction(signature, { symbol }) {
                                val isFakeOverride =
                                    firFunctionSymbol is FirNamedFunctionSymbol && firFunctionSymbol.isFakeOverride &&
                                            firFunctionSymbol.callableId != firFunctionSymbol.overriddenSymbol?.callableId
                                Fir2IrLazySimpleFunction(
                                    components, startOffset, endOffset, parentOrigin, firDeclaration, symbol, isFakeOverride
                                ).apply {
                                    parent = irParent
                                }
                            }
                        }
                        functionCache[firDeclaration] = irFunction
                        // NB: this is needed to prevent recursions in case of self bounds
                        (irFunction as Fir2IrLazySimpleFunction).prepareTypeParameters()
                        return symbol
                    }
                }
                createIrFunction(firDeclaration, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }.symbol
            }
            is FirConstructor -> {
                getIrConstructorSymbol(firDeclaration.symbol)
            }
            else -> error("Unknown kind of function: ${firDeclaration::class.java}: ${firDeclaration.render()}")
        }
    }

    fun getIrPropertyOrFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return it.symbol }
                val signature = signatureComposer.composeSignature(fir)
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                if (signature != null) {
                    symbolTable.referencePropertyIfAny(signature)?.let { irPropertySymbol ->
                        val irProperty = irPropertySymbol.owner
                        propertyCache[fir] = irProperty
                        return irPropertySymbol
                    }
                    // TODO: package fragment members (?)
                    if (irParent is Fir2IrLazyClass) {
                        assert(parentOrigin != IrDeclarationOrigin.DEFINED) {
                            "Should not have reference to public API uncached property from source code"
                        }
                        val symbol = Fir2IrPropertySymbol(signature, fir.containerSource)
                        val irProperty = fir.convertWithOffsets { startOffset, endOffset ->
                            symbolTable.declareProperty(signature, { symbol }) {
                                val isFakeOverride =
                                    firVariableSymbol is FirPropertySymbol && firVariableSymbol.isFakeOverride &&
                                            firVariableSymbol.callableId != firVariableSymbol.overriddenSymbol?.callableId
                                Fir2IrLazyProperty(
                                    components, startOffset, endOffset, parentOrigin, fir, symbol, isFakeOverride
                                ).apply {
                                    parent = irParent
                                }
                            }
                        }
                        propertyCache[fir] = irProperty
                        return symbol
                    }
                }
                createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }.symbol
            }
            is FirField -> {
                fieldCache[fir]?.let { return it.symbol }
                createIrField(fir).apply {
                    setAndModifyParent(findIrParent(fir))
                }.symbol
            }
            else -> throw IllegalArgumentException("Unexpected fir in property symbol: ${fir.render()}")
        }
    }

    fun getIrBackingFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return it.backingField!!.symbol }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }.backingField!!.symbol
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable<*>): IrVariableSymbol {
        return localStorage.getVariable(firVariable)?.symbol
            ?: throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                classifierStorage.getCachedIrEnumEntry(firDeclaration)?.let { return it.symbol }
                val containingFile = firProvider.getFirCallableContainerFile(firVariableSymbol)
                val parentClassSymbol = firVariableSymbol.callableId.classId?.let { firSymbolProvider.getClassLikeSymbolByFqName(it) }
                val irParentClass = (parentClassSymbol?.fir as? FirClass<*>)?.let { classifierStorage.getCachedIrClass(it) }
                classifierStorage.createIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    origin = if (containingFile != null) IrDeclarationOrigin.DEFINED else
                        (parentClassSymbol?.fir as? FirClass<*>)?.irOrigin(firProvider) ?: IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                ).symbol
            }
            is FirValueParameter -> {
                localStorage.getParameter(firDeclaration)?.symbol
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
            }
            else -> {
                getIrVariableSymbol(firDeclaration)
            }
        }
    }

    private fun IrSimpleFunction.populateOverriddenSymbols(thisReceiverOwner: IrClass) {
        thisReceiverOwner.findMatchingOverriddenSymbolsFromSupertypes(components.irBuiltIns, this)
            .filterIsInstance<IrSimpleFunctionSymbol>().let {
                if (it.isNotEmpty()) {
                    overriddenSymbols = it
                }
            }
    }

    private fun IrMutableAnnotationContainer.convertAnnotationsFromLibrary(firAnnotationContainer: FirAnnotationContainer) {
        if ((firAnnotationContainer as? FirDeclaration)?.isFromLibrary == true) {
            annotationGenerator?.generate(this, firAnnotationContainer)
        }
    }

    companion object {
        internal val ENUM_SYNTHETIC_NAMES = mapOf(
            Name.identifier("values") to IrSyntheticBodyKind.ENUM_VALUES,
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF
        )
    }
}