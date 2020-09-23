/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
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
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.isKFunctionInvoke
import org.jetbrains.kotlin.fir.symbols.Fir2IrConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrPropertySymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType

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
            declaration is IrAnonymousInitializer ||
            declaration is IrProperty ||
            declaration is IrEnumEntry
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(declaration: IrDeclaration) {
        if (declaration is IrSimpleFunction ||
            declaration is IrConstructor ||
            declaration is IrAnonymousInitializer ||
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

    private fun findIrClass(classId: ClassId): IrClass? =
        if (classId.isLocal) {
            classifierStorage.getCachedLocalClass(classId)
        } else {
            val firSymbol = firSymbolProvider.getClassLikeSymbolByFqName(classId)
            if (firSymbol is FirClassSymbol) {
                classifierStorage.getIrClassSymbol(firSymbol).owner
            } else {
                null
            }
        }

    internal fun findIrParent(packageFqName: FqName, parentClassId: ClassId?, firBasedSymbol: FirBasedSymbol<*>): IrDeclarationParent? {
        return if (parentClassId != null) {
            findIrClass(parentClassId)
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
                irFactory.createValueParameter(
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
                val receiverTypeRef =
                    if (function !is FirPropertyAccessor && function != null) function.receiverTypeRef
                    else parentPropertyReceiverType
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
                // See [LocalDeclarationsLowering]: "local function must not have dispatch receiver."
                val isLocal = function is FirSimpleFunction && function.isLocal
                if (function !is FirAnonymousFunction && containingClass != null && !isStatic && !isLocal) {
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
        return if (function !is FirSimpleFunction || function.visibility == Visibilities.Local) {
            localStorage.getLocalFunction(function)
        } else {
            functionCache[function]
        }
    }

    internal fun cacheIrSimpleFunction(function: FirSimpleFunction, irFunction: IrSimpleFunction) {
        functionCache[function] = irFunction
    }

    internal fun declareIrSimpleFunction(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction {
        if (signature == null) {
            val descriptor =
                if (containerSource != null) WrappedFunctionDescriptorWithContainerSource()
                else WrappedSimpleFunctionDescriptor()
            return symbolTable.declareSimpleFunction(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)
    }

    fun createIrFunction(
        function: FirFunction<*>,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
        isLocal: Boolean = false,
        containingClass: ConeClassLikeLookupTag? = null,
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
        val visibility = simpleFunction?.visibility ?: Visibilities.Local
        val isSuspend =
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.type?.isSuspendFunctionType(session) == true
            else simpleFunction?.isSuspend == true
        val signature = if (isLocal) null else signatureComposer.composeSignature(function, containingClass)
        val created = function.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrSimpleFunction(signature, simpleFunction?.containerSource) { symbol ->
                irFactory.createFunction(
                    startOffset, endOffset, updatedOrigin, symbol,
                    name, components.visibilityConverter.convertToDescriptorVisibility(visibility),
                    simpleFunction?.modality ?: Modality.FINAL,
                    function.returnTypeRef.toIrType(),
                    isInline = simpleFunction?.isInline == true,
                    isExternal = simpleFunction?.isExternal == true,
                    isTailrec = simpleFunction?.isTailRec == true,
                    isSuspend = isSuspend,
                    isExpect = simpleFunction?.isExpect == true,
                    isFakeOverride = updatedOrigin == IrDeclarationOrigin.FAKE_OVERRIDE,
                    isOperator = simpleFunction?.isOperator == true,
                    isInfix = simpleFunction?.isInfix == true,
                    containerSource = simpleFunction?.containerSource,
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

        if (visibility == Visibilities.Local) {
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
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
        isLocal: Boolean = false
    ): IrConstructor {
        val isPrimary = constructor.isPrimary
        classifierStorage.preCacheTypeParameters(constructor)
        val signature = if (isLocal) null else signatureComposer.composeSignature(constructor)
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            declareIrConstructor(signature) { symbol ->
                irFactory.createConstructor(
                    startOffset, endOffset, origin, symbol,
                    Name.special("<init>"), components.visibilityConverter.convertToDescriptorVisibility(constructor.visibility),
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
                if (isGetter) WrappedPropertyGetterDescriptor()
                else WrappedPropertySetterDescriptor()
            return symbolTable.declareSimpleFunction(descriptor, factory).apply { descriptor.bind(this) }
        }
        return symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)
    }

    internal fun createIrPropertyAccessor(
        propertyAccessor: FirPropertyAccessor?,
        property: FirProperty,
        correspondingProperty: IrDeclarationWithName,
        propertyType: IrType,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int,
        isLocal: Boolean = false,
        containingClass: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunction {
        val prefix = if (isSetter) "set" else "get"
        val signature = if (isLocal) null else signatureComposer.composeAccessorSignature(property, isSetter, containingClass)
        val containerSource = (correspondingProperty as? IrProperty)?.containerSource
        return declareIrAccessor(
            signature,
            containerSource,
            isGetter = !isSetter
        ) { symbol ->
            val accessorReturnType = if (isSetter) irBuiltIns.unitType else propertyType
            val visibility = propertyAccessor?.visibility?.let {
                components.visibilityConverter.convertToDescriptorVisibility(it)
            }
            irFactory.createFunction(
                startOffset, endOffset, origin, symbol,
                Name.special("<$prefix-${correspondingProperty.name}>"),
                visibility ?: (correspondingProperty as IrDeclarationWithVisibility).visibility,
                (correspondingProperty as? IrOverridableMember)?.modality ?: Modality.FINAL, accessorReturnType,
                isInline = propertyAccessor?.isInline == true,
                isExternal = propertyAccessor?.isExternal == true,
                isTailrec = false, isSuspend = false, isOperator = false,
                isInfix = false,
                isExpect = false, isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                containerSource = containerSource,
            ).apply {
                correspondingPropertySymbol = (correspondingProperty as? IrProperty)?.symbol
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
                if (correspondingProperty is Fir2IrLazyProperty && !isFakeOverride && thisReceiverOwner != null) {
                    this.overriddenSymbols = correspondingProperty.fir.generateOverriddenAccessorSymbols(
                        correspondingProperty.containingClass, !isSetter, session, scopeSession, declarationStorage
                    )
                }
            }
        }
    }

    internal fun IrProperty.createBackingField(
        property: FirProperty,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        visibility: DescriptorVisibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField {
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType()
        return symbolTable.declareField(
            startOffset, endOffset, origin, descriptor, inferredType
        ) { symbol ->
            irFactory.createField(
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
            else -> Visibilities.Private
        }

    private fun declareIrProperty(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrPropertySymbol) -> IrProperty
    ): IrProperty {
        if (signature == null) {
            val descriptor =
                if (containerSource != null) WrappedPropertyDescriptorWithContainerSource()
                else WrappedPropertyDescriptor()
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
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
        isLocal: Boolean = false,
        containingClass: ConeClassLikeLookupTag? = null,
    ): IrProperty {
        classifierStorage.preCacheTypeParameters(property)
        val signature = if (isLocal) null else signatureComposer.composeSignature(property, containingClass)
        return property.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrProperty(signature, property.containerSource) { symbol ->
                irFactory.createProperty(
                    startOffset, endOffset, origin, symbol,
                    property.name, components.visibilityConverter.convertToDescriptorVisibility(property.visibility), property.modality!!,
                    isVar = property.isVar,
                    isConst = property.isConst,
                    isLateinit = property.isLateInit,
                    isDelegated = property.delegate != null,
                    isExternal = property.isExternal,
                    isExpect = property.isExpect,
                    isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                    containerSource = property.containerSource,
                ).apply {
                    metadata = FirMetadataSource.Property(property)
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
                    if (property.isConst || (property.modality != Modality.ABSTRACT && (irParent !is IrClass || !irParent.isInterface))) {
                        if (property.hasBackingField) {
                            backingField = if (delegate != null) {
                                createBackingField(
                                    property, IrDeclarationOrigin.PROPERTY_DELEGATE, descriptor,
                                    components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                    Name.identifier("${property.name}\$delegate"), true, delegate
                                )
                            } else {
                                createBackingField(
                                    property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD, descriptor,
                                    components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                    property.name, property.isVal, initializer, type
                                ).also { field ->
                                    if (initializer is FirConstExpression<*>) {
                                        // TODO: Normally we shouldn't have error type here
                                        val constType = initializer.typeRef.toIrType().takeIf { it !is IrErrorType } ?: type
                                        field.initializer = factory.createExpressionBody(initializer.toIrConst(constType))
                                    }
                                }
                            }
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
                        startOffset, endOffset, isLocal, containingClass
                    )
                    if (property.isVar) {
                        this.setter = createIrPropertyAccessor(
                            setter, property, this, type, irParent, thisReceiverOwner, true,
                            when {
                                delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset, isLocal, containingClass
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

    internal fun cacheIrProperty(property: FirProperty, irProperty: IrProperty) {
        propertyCache[property] = irProperty
    }

    fun getCachedIrField(field: FirField): IrField? = fieldCache[field]

    fun createIrFieldAndDelegatedMembers(field: FirField, owner: FirClass<*>, irClass: IrClass): IrField {
        val irField = createIrField(field, origin = IrDeclarationOrigin.DELEGATE)
        irField.setAndModifyParent(irClass)
        delegatedMemberGenerator.generate(irField, owner, irClass)
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
        val declaration = firSymbolProvider.getClassLikeSymbolByFqName(classId)
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
                irFactory.createField(
                    startOffset, endOffset, origin, symbol,
                    field.name, type, components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                    isFinal = field.modality == Modality.FINAL,
                    isExternal = false,
                    isStatic = field.isStatic
                ).apply {
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
                irFactory.createValueParameter(
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
                        this.defaultValue = factory.createExpressionBody(
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

    fun createIrLocalDelegatedProperty(property: FirProperty, irParent: IrDeclarationParent): IrLocalDelegatedProperty {
        val type = property.returnTypeRef.toIrType()
        val origin = IrDeclarationOrigin.DEFINED
        val irProperty = property.convertWithOffsets { startOffset, endOffset ->
            val descriptor = WrappedVariableDescriptorWithAccessor()
            symbolTable.declareLocalDelegatedProperty(startOffset, endOffset, origin, descriptor, type) {
                irFactory.createLocalDelegatedProperty(startOffset, endOffset, origin, it, property.name, type, property.isVar).apply {
                    descriptor.bind(this)
                }
            }
        }.apply {
            parent = irParent
            metadata = FirMetadataSource.Property(property)
            enterScope(this)
            delegate = declareIrVariable(
                startOffset, endOffset, IrDeclarationOrigin.PROPERTY_DELEGATE,
                Name.identifier("${property.name}\$delegate"), property.delegate!!.typeRef.toIrType(),
                isVar = false, isConst = false, isLateinit = false
            )
            delegate.parent = irParent
            getter = createIrPropertyAccessor(
                property.getter, property, this, type, irParent, null, false,
                IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, isLocal
            )
            if (property.isVar) {
                setter = createIrPropertyAccessor(
                    property.setter, property, this, type, irParent, null, true,
                    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, isLocal
                )
            }
            leaveScope(this)
        }
        localStorage.putDelegatedProperty(property, irProperty)
        return irProperty
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
                                    components, startOffset, endOffset, parentOrigin, firDeclaration, irParent.fir, symbol, isFakeOverride
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

    fun getIrPropertySymbol(firPropertySymbol: FirPropertySymbol): IrSymbol {
        val fir = firPropertySymbol.fir
        if (fir.isLocal) {
            return localStorage.getDelegatedProperty(fir)?.symbol ?: getIrVariableSymbol(fir)
        }
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
                            firPropertySymbol.isFakeOverride &&
                                    firPropertySymbol.callableId != firPropertySymbol.overriddenSymbol?.callableId
                        Fir2IrLazyProperty(
                            components, startOffset, endOffset, parentOrigin, fir, irParent.fir, symbol, isFakeOverride
                        ).apply {
                            parent = irParent
                        }
                    }
                }
                propertyCache[fir] = irProperty
                return symbol
            }
        }
        return createIrProperty(fir, irParent, origin = parentOrigin).apply {
            setAndModifyParent(irParent)
        }.symbol
    }

    fun getIrFieldSymbol(firFieldSymbol: FirFieldSymbol): IrSymbol {
        val fir = firFieldSymbol.fir
        val irProperty = fieldCache[fir] ?: createIrField(fir).apply {
            setAndModifyParent(findIrParent(fir))
        }
        return irProperty.symbol
    }

    fun getIrBackingFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                if (fir.isLocal) {
                    return localStorage.getDelegatedProperty(fir)?.delegate?.symbol ?: getIrVariableSymbol(fir)
                }
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
                val irParentClass = firVariableSymbol.callableId.classId?.let { findIrClass(it) }
                classifierStorage.createIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    origin = if (containingFile != null) IrDeclarationOrigin.DEFINED else
                        irParentClass?.origin ?: IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
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
