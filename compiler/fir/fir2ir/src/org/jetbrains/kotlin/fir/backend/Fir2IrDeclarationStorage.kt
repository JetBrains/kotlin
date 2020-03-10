/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

class Fir2IrDeclarationStorage(
    private val session: FirSession,
    private val irSymbolTable: SymbolTable,
    private val moduleDescriptor: FirModuleDescriptor
) {
    private val firSymbolProvider = session.firSymbolProvider

    private val firProvider = session.firProvider

    private val fragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val fileCache = mutableMapOf<FirFile, IrFile>()

    private val classCache = mutableMapOf<FirRegularClass, IrClass>()

    private val typeParameterCache = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val typeParameterCacheForSetter = mutableMapOf<FirTypeParameter, IrTypeParameter>()

    private val functionCache = mutableMapOf<FirFunction<*>, IrSimpleFunction>()

    private val constructorCache = mutableMapOf<FirConstructor, IrConstructor>()

    private val propertyCache = mutableMapOf<FirProperty, IrProperty>()

    private val fieldCache = mutableMapOf<FirField, IrField>()

    private val enumEntryCache = mutableMapOf<FirEnumEntry, IrEnumEntry>()

    private val localStorage = Fir2IrLocalStorage()

    lateinit var typeConverter: Fir2IrTypeConverter

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

    fun enterScope(descriptor: DeclarationDescriptor) {
        irSymbolTable.enterScope(descriptor)
        if (descriptor is WrappedSimpleFunctionDescriptor ||
            descriptor is WrappedClassConstructorDescriptor ||
            descriptor is WrappedPropertyDescriptor ||
            descriptor is WrappedEnumEntryDescriptor
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(descriptor: DeclarationDescriptor) {
        if (descriptor is WrappedSimpleFunctionDescriptor ||
            descriptor is WrappedClassConstructorDescriptor ||
            descriptor is WrappedPropertyDescriptor
        ) {
            localStorage.leaveCallable()
        }
        irSymbolTable.leaveScope(descriptor)
    }

    private fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun ConeKotlinType.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun getIrExternalPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return fragmentCache.getOrPut(fqName) {
            // TODO: module descriptor is wrong here
            return irSymbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
        }
    }

    private fun IrDeclaration.declareThisReceiverParameter(
        parent: IrDeclarationParent,
        thisType: IrType,
        thisOrigin: IrDeclarationOrigin,
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset
    ): IrValueParameter {
        val receiverDescriptor = WrappedReceiverParameterDescriptor()
        return irSymbolTable.declareValueParameter(
            startOffset, endOffset, thisOrigin, receiverDescriptor, thisType
        ) { symbol ->
            IrValueParameterImpl(
                startOffset, endOffset, thisOrigin, symbol,
                Name.special("<this>"), -1, thisType,
                varargElementType = null, isCrossinline = false, isNoinline = false
            ).apply {
                this.parent = parent
                receiverDescriptor.bind(this)
            }
        }
    }

    private fun IrClass.setThisReceiver() {
        enterScope(descriptor)
        val typeArguments = this.typeParameters.map {
            IrSimpleTypeImpl(it.symbol, false, emptyList(), emptyList())
        }
        thisReceiver = declareThisReceiverParameter(
            parent = this,
            thisType = IrSimpleTypeImpl(symbol, false, typeArguments, emptyList()),
            thisOrigin = IrDeclarationOrigin.INSTANCE_RECEIVER
        )
        leaveScope(descriptor)
    }

    private fun preCacheTypeParameters(owner: FirTypeParametersOwner) {
        owner.typeParameters.mapIndexed { index, typeParameter ->
            getIrTypeParameter(typeParameter, index)
            if (owner is FirProperty && owner.isVar) {
                getIrTypeParameter(typeParameter, index, ConversionTypeContext.DEFAULT.inSetter())
            }
        }
    }

    private fun IrTypeParametersContainer.setTypeParameters(
        owner: FirTypeParametersOwner,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ) {
        typeParameters = owner.typeParameters.mapIndexed { index, typeParameter ->
            getIrTypeParameter(typeParameter, index, typeContext).apply { parent = this@setTypeParameters }
        }
    }

    private fun IrClass.declareSupertypesAndTypeParameters(klass: FirClass<*>): IrClass {
        if (klass is FirRegularClass) {
            setTypeParameters(klass)
        }
        superTypes = klass.superTypeRefs.map { superTypeRef -> superTypeRef.toIrType() }
        return this
    }

    private fun addDeclarationsToExternalClass(regularClass: FirRegularClass, irClass: IrClass) {
        if (regularClass.symbol.classId.packageFqName.startsWith(Name.identifier("kotlin"))) {
            // Note: yet this is necessary only for *Range / *Progression classes
            // due to BE optimizations (for lowering) that use their first / last / step members
            // TODO: think how to refactor this piece of code and/or merge it with similar Fir2IrVisitor fragment
            val processedNames = mutableSetOf<Name>()
            for (declaration in regularClass.declarations) {
                irClass.declarations += when (declaration) {
                    is FirSimpleFunction -> {
                        processedNames += declaration.name
                        getIrFunction(declaration, irClass)
                    }
                    is FirProperty -> {
                        processedNames += declaration.name
                        getIrProperty(declaration, irClass)
                    }
                    is FirConstructor -> getIrConstructor(declaration, irClass)
                    is FirRegularClass -> getIrClass(declaration, exactlySourceClass = false)
                    else -> continue
                }
            }
            val allNames = regularClass.collectCallableNamesFromSupertypes(session)
            val scope = regularClass.buildUseSiteMemberScope(session, ScopeSession())
            if (scope != null) {
                for (name in allNames) {
                    if (name in processedNames) continue
                    processedNames += name
                    scope.processFunctionsByName(name) { functionSymbol ->
                        if (functionSymbol is FirNamedFunctionSymbol) {
                            val fakeOverrideSymbol =
                                FirClassSubstitutionScope.createFakeOverrideFunction(session, functionSymbol.fir, functionSymbol)
                            irClass.declarations += getIrFunction(fakeOverrideSymbol.fir, irClass)
                        }
                    }
                    scope.processPropertiesByName(name) { propertySymbol ->
                        if (propertySymbol is FirPropertySymbol) {
                            val fakeOverrideSymbol =
                                FirClassSubstitutionScope.createFakeOverrideProperty(session, propertySymbol.fir, propertySymbol)
                            irClass.declarations += getIrProperty(fakeOverrideSymbol.fir, irClass)
                        }
                    }
                }
            }
            for (irDeclaration in irClass.declarations) {
                irDeclaration.parent = irClass
            }
        }
    }

    private fun getCachedIrClass(klass: FirClass<*>): IrClass? {
        return if (klass is FirAnonymousObject || klass is FirRegularClass && klass.visibility == Visibilities.LOCAL) {
            localStorage.getLocalClass(klass)
        } else {
            classCache[klass]
        }
    }

    private fun FirRegularClass.enumClassModality(): Modality {
        return when {
            declarations.any { it is FirCallableMemberDeclaration<*> && it.modality == Modality.ABSTRACT } -> {
                Modality.ABSTRACT
            }
            declarations.any { it is FirEnumEntry && it.initializer != null } -> {
                Modality.OPEN
            }
            else -> {
                Modality.FINAL
            }
        }
    }

    private fun createIrClass(klass: FirClass<*>, exactlySourceClass: Boolean = false): IrClass {
        // NB: klass can be either FirRegularClass or FirAnonymousObject
        if (klass is FirAnonymousObject) {
            return createIrAnonymousObject(klass)
        }
        val regularClass = klass as FirRegularClass

        val descriptor = WrappedClassDescriptor()
        val origin =
            if (exactlySourceClass || firProvider.getFirClassifierContainerFileIfAny(klass.symbol) != null) IrDeclarationOrigin.DEFINED
            else IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        val visibility = regularClass.visibility
        val modality = if (regularClass.classKind == ClassKind.ENUM_CLASS) {
            regularClass.enumClassModality()
        } else {
            regularClass.modality ?: Modality.FINAL
        }
        val irClass = klass.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality, visibility) { symbol ->
                IrClassImpl(
                    startOffset,
                    endOffset,
                    origin,
                    symbol,
                    regularClass.name,
                    klass.classKind,
                    visibility,
                    modality,
                    isCompanion = regularClass.isCompanion,
                    isInner = regularClass.isInner,
                    isData = regularClass.isData,
                    isExternal = regularClass.isExternal,
                    isInline = regularClass.isInline,
                    isExpect = regularClass.isExpect,
                    isFun = false // TODO FirRegularClass.isFun
                ).apply {
                    descriptor.bind(this)
                }
            }
        }
        if (regularClass.visibility == Visibilities.LOCAL) {
            localStorage.putLocalClass(klass, irClass)
        } else {
            classCache[klass] = irClass
        }
        irClass.declareSupertypesAndTypeParameters(klass)
        irClass.setThisReceiver()
        return irClass
    }

    fun getIrClass(klass: FirClass<*>, exactlySourceClass: Boolean = true): IrClass {
        return getCachedIrClass(klass) ?: createIrClass(klass, exactlySourceClass)
    }

    private fun createIrAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.LOCAL,
        name: Name = Name.special("<no name provided>"),
        irParent: IrClass? = null
    ): IrClass {
        val descriptor = WrappedClassDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val modality = Modality.FINAL
        val result = anonymousObject.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareClass(startOffset, endOffset, origin, descriptor, modality, visibility) { symbol ->
                IrClassImpl(
                    startOffset, endOffset, origin, symbol, name,
                    // NB: for unknown reason, IR uses 'CLASS' kind for simple anonymous objects
                    anonymousObject.classKind.takeIf { it == ClassKind.ENUM_ENTRY } ?: ClassKind.CLASS,
                    visibility, modality,
                    isCompanion = false, isInner = false, isData = false,
                    isExternal = false, isInline = false, isExpect = false, isFun = false
                ).apply {
                    descriptor.bind(this)
                    setThisReceiver()
                    if (irParent != null) {
                        this.parent = irParent
                    }
                }
            }
        }.declareSupertypesAndTypeParameters(anonymousObject)
        localStorage.putLocalClass(anonymousObject, result)
        return result
    }

    fun getIrAnonymousObject(
        anonymousObject: FirAnonymousObject,
        visibility: Visibility = Visibilities.LOCAL,
        name: Name = Name.special("<no name provided>"),
        irParent: IrClass? = null
    ): IrClass {
        localStorage.getLocalClass(anonymousObject)?.let { return it }
        return createIrAnonymousObject(anonymousObject, visibility, name, irParent)
    }

    private fun getIrEnumEntryClass(enumEntry: FirEnumEntry, anonymousObject: FirAnonymousObject, irParent: IrClass?): IrClass {
        return getIrAnonymousObject(anonymousObject, Visibilities.PRIVATE, enumEntry.name, irParent)
    }

    private fun getIrTypeParameter(
        typeParameter: FirTypeParameter,
        index: Int = -1,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrTypeParameter {
        // Here transformation is a bit difficult because one FIR property type parameter
        // can be transformed to two different type parameters: one for getter and another one for setter
        val simpleCachedParameter = typeParameterCache[typeParameter]
        if (simpleCachedParameter != null) {
            if (typeContext.origin != ConversionTypeOrigin.SETTER) {
                return simpleCachedParameter
            }
            if (index < 0) {
                val parent = simpleCachedParameter.parent
                if (parent !is IrSimpleFunction || parent.returnType == typeConverter.unitType) {
                    return simpleCachedParameter
                }
            }
        }
        if (typeContext.origin == ConversionTypeOrigin.SETTER) {
            typeParameterCacheForSetter[typeParameter]?.let { return it }
        }
        return typeParameter.run {
            // Yet I don't want to enable this requirement because it breaks some tests
            // However, if we get here it *should* mean that type parameter index is given explicitly
            // At this moment (20.02.2020) this requirement breaks 11/355 Fir2IrText tests
            // require(index != -1)
            val descriptor = WrappedTypeParameterDescriptor()
            val origin = IrDeclarationOrigin.DEFINED
            val irTypeParameter =
                convertWithOffsets { startOffset, endOffset ->
                    irSymbolTable.declareGlobalTypeParameter(startOffset, endOffset, origin, descriptor) { symbol ->
                        IrTypeParameterImpl(
                            startOffset, endOffset, origin, symbol,
                            name, if (index < 0) 0 else index,
                            isReified,
                            variance
                        ).apply {
                            descriptor.bind(this)
                        }
                    }
                }

            // Cache the type parameter BEFORE processing its bounds/supertypes, to properly handle recursive type bounds.
            if (typeContext.origin == ConversionTypeOrigin.SETTER) {
                typeParameterCacheForSetter[typeParameter] = irTypeParameter
            } else {
                typeParameterCache[typeParameter] = irTypeParameter
            }
            bounds.mapTo(irTypeParameter.superTypes) { it.toIrType() }
            irTypeParameter
        }
    }

    private fun findIrParent(packageFqName: FqName, parentClassId: ClassId?, firBasedSymbol: FirBasedSymbol<*>): IrDeclarationParent? {
        return if (parentClassId != null) {
            // TODO: this will never work for local classes
            val parentFirSymbol = firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)
            if (parentFirSymbol is FirClassSymbol) {
                val parentIrSymbol = getIrClassSymbol(parentFirSymbol)
                parentIrSymbol.owner
            } else {
                null
            }
        } else {
            val containerFile = when (firBasedSymbol) {
                is FirCallableSymbol -> firProvider.getFirCallableContainerFile(firBasedSymbol)
                is FirClassLikeSymbol -> firProvider.getFirClassifierContainerFileIfAny(firBasedSymbol)
                else -> throw AssertionError("Unexpected: $firBasedSymbol")
            }

            if (containerFile != null) {
                fileCache[containerFile]
            } else {
                getIrExternalPackageFragment(packageFqName)
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
            irSymbolTable.declareValueParameter(
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
        if (function is FirSimpleFunction) {
            setTypeParameters(function)
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
                createAndSaveIrParameter(
                    valueParameter, index,
                    useStubForDefaultValueStub = function !is FirConstructor || containingClass?.name != Name.identifier("Enum"),
                    typeContext
                ).apply {
                    this.parent = parent
                }
            }
        }
        if (function !is FirConstructor) {
            val thisOrigin = IrDeclarationOrigin.DEFINED
            val receiverTypeRef = if (function !is FirPropertyAccessor) function?.receiverTypeRef else parentPropertyReceiverType
            if (receiverTypeRef != null) {
                extensionReceiverParameter = receiverTypeRef.convertWithOffsets { startOffset, endOffset ->
                    declareThisReceiverParameter(
                        parent,
                        thisType = receiverTypeRef.toIrType(typeContext),
                        thisOrigin = thisOrigin,
                        startOffset = startOffset,
                        endOffset = endOffset
                    )
                }
            }
            if (function !is FirAnonymousFunction && containingClass != null && !isStatic) {
                dispatchReceiverParameter = declareThisReceiverParameter(
                    parent,
                    thisType = containingClass.thisReceiver!!.type,
                    thisOrigin = thisOrigin
                )
            }
        }
    }

    private fun <T : IrFunction> T.bindAndDeclareParameters(
        function: FirFunction<*>?,
        descriptor: WrappedCallableDescriptor<T>,
        irParent: IrDeclarationParent?,
        isStatic: Boolean,
        parentPropertyReceiverType: FirTypeRef? = null
    ): T {
        if (irParent != null) {
            parent = irParent
        }
        descriptor.bind(this)
        declareParameters(function, irParent as? IrClass, isStatic, parentPropertyReceiverType)
        return this
    }

    fun <T : IrFunction> T.enterLocalScope(function: FirFunction<*>): T {
        enterScope(descriptor)
        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters)) {
            irSymbolTable.introduceValueParameter(irParameter)
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    private fun getCachedIrFunction(function: FirFunction<*>): IrSimpleFunction? {
        return if (function !is FirSimpleFunction || function.visibility == Visibilities.LOCAL) {
            localStorage.getLocalFunction(function)
        } else {
            functionCache[function]
        }
    }

    private fun createIrFunction(
        function: FirFunction<*>,
        irParent: IrDeclarationParent?,
        shouldLeaveScope: Boolean = true,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrSimpleFunction {
        val simpleFunction = function as? FirSimpleFunction
        val containerSource = simpleFunction?.containerSource
        val descriptor = containerSource?.let { WrappedFunctionDescriptorWithContainerSource(it) } ?: WrappedSimpleFunctionDescriptor()
        val isLambda = function.psi is KtFunctionLiteral
        val updatedOrigin = when {
            isLambda -> IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            function.symbol.callableId.isKFunctionInvoke() -> IrDeclarationOrigin.FAKE_OVERRIDE
            else -> origin
        }
        preCacheTypeParameters(function)
        val name = simpleFunction?.name
            ?: if (isLambda) Name.special("<anonymous>") else Name.special("<no name provided>")
        val visibility = simpleFunction?.visibility ?: Visibilities.LOCAL
        val created = function.convertWithOffsets { startOffset, endOffset ->
            enterScope(descriptor)
            val result = irSymbolTable.declareSimpleFunction(startOffset, endOffset, origin, descriptor) { symbol ->
                IrFunctionImpl(
                    startOffset, endOffset, updatedOrigin, symbol,
                    name, visibility,
                    simpleFunction?.modality ?: Modality.FINAL,
                    function.returnTypeRef.toIrType(),
                    isInline = simpleFunction?.isInline == true,
                    isExternal = simpleFunction?.isExternal == true,
                    isTailrec = simpleFunction?.isTailRec == true,
                    isSuspend = simpleFunction?.isSuspend == true,
                    isExpect = simpleFunction?.isExpect == true,
                    isFakeOverride = updatedOrigin == IrDeclarationOrigin.FAKE_OVERRIDE,
                    isOperator = simpleFunction?.isOperator == true
                )
            }
            result
        }.bindAndDeclareParameters(function, descriptor, irParent, isStatic = simpleFunction?.isStatic == true)

        if (shouldLeaveScope) {
            leaveScope(created.descriptor)
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

    fun getIrFunction(
        function: FirFunction<*>,
        irParent: IrDeclarationParent?,
        shouldLeaveScope: Boolean = true,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrSimpleFunction {
        return getCachedIrFunction(function)?.apply {
            if (!shouldLeaveScope) enterLocalScope(function)
        } ?: createIrFunction(function, irParent, shouldLeaveScope, origin)
    }

    private fun getCachedIrConstructor(
        constructor: FirConstructor
    ): IrConstructor? {
        return constructorCache[constructor]
    }

    private fun createIrConstructor(
        constructor: FirConstructor,
        irParent: IrDeclarationParent,
        shouldLeaveScope: Boolean = true,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrConstructor {
        val descriptor = WrappedClassConstructorDescriptor()
        val isPrimary = constructor.isPrimary
        val visibility = when {
            irParent is IrClass && irParent.modality == Modality.SEALED -> Visibilities.PRIVATE
            else -> constructor.visibility
        }
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareConstructor(startOffset, endOffset, origin, descriptor) { symbol ->
                IrConstructorImpl(
                    startOffset, endOffset, origin, symbol,
                    Name.special("<init>"), visibility,
                    constructor.returnTypeRef.toIrType(),
                    isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = false
                ).apply {
                    enterScope(descriptor)
                }.bindAndDeclareParameters(constructor, descriptor, irParent, isStatic = true).apply {
                    if (shouldLeaveScope) {
                        leaveScope(descriptor)
                    }
                }
            }
        }
        constructorCache[constructor] = created
        return created
    }

    fun getIrConstructor(
        constructor: FirConstructor,
        irParent: IrDeclarationParent,
        shouldLeaveScope: Boolean = true
    ): IrConstructor {
        val cached = getCachedIrConstructor(constructor)
        if (cached != null) {
            return if (shouldLeaveScope) cached else cached.enterLocalScope(constructor)
        }
        return createIrConstructor(constructor, irParent, shouldLeaveScope)
    }

    private fun createIrPropertyAccessor(
        propertyAccessor: FirPropertyAccessor?,
        property: FirProperty,
        correspondingProperty: IrProperty,
        propertyType: IrType,
        irParent: IrDeclarationParent?,
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int
    ): IrSimpleFunction {
        val propertyDescriptor = correspondingProperty.descriptor
        val descriptor =
            if (propertyDescriptor is WrappedPropertyDescriptorWithContainerSource)
                WrappedFunctionDescriptorWithContainerSource(propertyDescriptor.containerSource)
            else WrappedSimpleFunctionDescriptor()
        val prefix = if (isSetter) "set" else "get"
        return irSymbolTable.declareSimpleFunction(
            propertyAccessor?.psi?.startOffsetSkippingComments ?: startOffset,
            propertyAccessor?.psi?.endOffset ?: endOffset,
            origin, descriptor
        ) { symbol ->
            val accessorReturnType = if (isSetter) typeConverter.unitType else propertyType
            IrFunctionImpl(
                startOffset, endOffset, origin, symbol,
                Name.special("<$prefix-${correspondingProperty.name}>"),
                propertyAccessor?.visibility ?: correspondingProperty.visibility,
                correspondingProperty.modality, accessorReturnType,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isExpect = false,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                isOperator = false
            ).apply {
                setTypeParameters(
                    property, ConversionTypeContext(
                        definitelyNotNull = false,
                        origin = if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
                    )
                )
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeContext.DEFAULT.inSetter())
                    )
                }
                enterScope(descriptor)
            }.bindAndDeclareParameters(
                propertyAccessor, descriptor, irParent, isStatic = irParent !is IrClass,
                parentPropertyReceiverType = property.receiverTypeRef
            ).apply {
                leaveScope(descriptor)
                if (irParent != null) {
                    parent = irParent
                }
                correspondingPropertySymbol = correspondingProperty.symbol
            }
        }
    }


    fun getIrEnumEntry(
        enumEntry: FirEnumEntry,
        irParent: IrClass? = null,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrEnumEntry {
        return enumEntryCache.getOrPut(enumEntry) {
            enumEntry.convertWithOffsets { startOffset, endOffset ->
                val desc = WrappedEnumEntryDescriptor()
                enterScope(desc)
                val result = irSymbolTable.declareEnumEntry(startOffset, endOffset, origin, desc) { symbol ->
                    IrEnumEntryImpl(
                        startOffset, endOffset, origin, symbol, enumEntry.name
                    ).apply {
                        desc.bind(this)
                        val irType = enumEntry.returnTypeRef.toIrType()
                        if (irParent != null) {
                            this.parent = irParent
                        }
                        val initializer = enumEntry.initializer
                        if (initializer != null) {
                            initializer as FirAnonymousObject
                            val klass = getIrEnumEntryClass(enumEntry, initializer, irParent)

                            this.correspondingClass = klass
                        } else if (irParent != null) {
                            this.initializerExpression = IrExpressionBodyImpl(
                                IrEnumConstructorCallImpl(startOffset, endOffset, irType, irParent.constructors.first().symbol)
                            )
                        }
                    }
                }
                leaveScope(desc)
                result
            }
        }
    }

    private fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
        shouldLeaveScope: Boolean = true
    ): IrProperty {
        val containerSource = property.containerSource
        val descriptor = containerSource?.let { WrappedPropertyDescriptorWithContainerSource(it) } ?: WrappedPropertyDescriptor()
        preCacheTypeParameters(property)
        return property.convertWithOffsets { startOffset, endOffset ->
            enterScope(descriptor)
            val result = irSymbolTable.declareProperty(
                startOffset, endOffset,
                origin, descriptor, property.delegate != null
            ) { symbol ->
                IrPropertyImpl(
                    startOffset, endOffset, origin, symbol,
                    property.name, property.visibility, property.modality!!,
                    isVar = property.isVar,
                    isConst = property.isConst,
                    isLateinit = property.isLateInit,
                    isDelegated = property.delegate != null,
                    // TODO
                    isExternal = false,
                    isExpect = property.isExpect,
                    isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
                ).apply {
                    descriptor.bind(this)
                    if (irParent != null) {
                        parent = irParent
                    }
                    val type = property.returnTypeRef.toIrType()
                    getter = createIrPropertyAccessor(
                        property.getter, property, this, type, irParent, false,
                        when {
                            origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                            property.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                            property.getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                            else -> origin
                        },
                        startOffset, endOffset
                    )
                    if (property.isVar) {
                        setter = createIrPropertyAccessor(
                            property.setter, property, this, type, irParent, true,
                            when {
                                property.delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                property.setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset
                        )
                    }
                }
            }
            if (shouldLeaveScope) {
                leaveScope(descriptor)
            }
            propertyCache[property] = result
            result
        }
    }

    fun getIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED,
        shouldLeaveScope: Boolean = true
    ): IrProperty {
        val cached = propertyCache[property]
        if (cached != null) {
            if (!shouldLeaveScope) {
                enterScope(cached.descriptor)
            }
            return cached
        }
        return createIrProperty(property, irParent, origin, shouldLeaveScope)
    }

    private fun createIrField(field: FirField): IrField {
        val descriptor = WrappedFieldDescriptor()
        val origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        val type = field.returnTypeRef.toIrType()
        return field.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareField(
                startOffset, endOffset,
                origin, descriptor, type
            ) { symbol ->
                IrFieldImpl(
                    startOffset, endOffset, origin, symbol,
                    field.name, type, field.visibility,
                    isFinal = field.modality == Modality.FINAL,
                    isExternal = false,
                    isStatic = field.isStatic,
                    isFakeOverride = false
                ).apply {
                    descriptor.bind(this)
                    fieldCache[field] = this
                }
            }
        }
    }

    private fun createAndSaveIrParameter(
        valueParameter: FirValueParameter,
        index: Int = -1,
        useStubForDefaultValueStub: Boolean = true,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
    ): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        val origin = IrDeclarationOrigin.DEFINED
        val type = valueParameter.returnTypeRef.toIrType()
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            irSymbolTable.declareValueParameter(
                startOffset, endOffset, origin, descriptor, type
            ) { symbol ->
                IrValueParameterImpl(
                    startOffset, endOffset, origin, symbol,
                    valueParameter.name, index, type,
                    if (!valueParameter.isVararg) null
                    else valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.arrayElementType(session)?.toIrType(typeContext),
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
        return irSymbolTable.declareVariable(startOffset, endOffset, origin, descriptor, type) { symbol ->
            IrVariableImpl(
                startOffset, endOffset, origin, symbol, name, type,
                isVar, isConst, isLateinit
            ).apply {
                descriptor.bind(this)
            }
        }
    }

    fun createAndSaveIrVariable(variable: FirVariable<*>, givenOrigin: IrDeclarationOrigin? = null): IrVariable {
        val type = variable.returnTypeRef.toIrType()
        // Some temporary variables are produced in RawFirBuilder, but we consistently use special names for them.
        val origin = when {
            givenOrigin != null -> givenOrigin
            variable.name == Name.special("<iterator>") -> IrDeclarationOrigin.FOR_LOOP_ITERATOR
            variable.name.isSpecial -> IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            else -> IrDeclarationOrigin.DEFINED
        }
        val irVariable = variable.convertWithOffsets { startOffset, endOffset ->
            declareIrVariable(
                startOffset, endOffset, origin,
                variable.name, type, variable.isVar, isConst = false, isLateinit = false
            )
        }
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

    fun getIrClassSymbol(firClassSymbol: FirClassSymbol<*>): IrClassSymbol {
        val firClass = firClassSymbol.fir
        getCachedIrClass(firClass)?.let { return irSymbolTable.referenceClass(it.descriptor) }
        val irClass = createIrClass(firClass)
        if (firClass is FirAnonymousObject || firClass is FirRegularClass && firClass.visibility == Visibilities.LOCAL) {
            return irSymbolTable.referenceClass(irClass.descriptor)
        }
        val classId = firClassSymbol.classId
        val parentId = classId.outerClassId
        val irParent = findIrParent(classId.packageFqName, parentId, firClassSymbol)
        if (irParent != null) {
            irClass.parent = irParent
        }
        if (irParent is IrExternalPackageFragment) {
            addDeclarationsToExternalClass(firClass as FirRegularClass, irClass)
        }

        return irSymbolTable.referenceClass(irClass.descriptor)
    }

    fun getIrTypeParameterSymbol(
        firTypeParameterSymbol: FirTypeParameterSymbol,
        typeContext: ConversionTypeContext
    ): IrTypeParameterSymbol {
        val irTypeParameter = getIrTypeParameter(firTypeParameterSymbol.fir, typeContext = typeContext)
        return irSymbolTable.referenceTypeParameter(irTypeParameter.descriptor)
    }

    fun getIrFunctionSymbol(firFunctionSymbol: FirFunctionSymbol<*>): IrFunctionSymbol {
        return when (val firDeclaration = firFunctionSymbol.fir) {
            is FirSimpleFunction, is FirAnonymousFunction -> {
                getCachedIrFunction(firDeclaration)?.let { return irSymbolTable.referenceSimpleFunction(it.descriptor) }
                val irParent = findIrParent(firDeclaration)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irDeclaration = createIrFunction(firDeclaration, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceSimpleFunction(irDeclaration.descriptor)
            }
            is FirConstructor -> {
                getCachedIrConstructor(firDeclaration)?.let { return irSymbolTable.referenceConstructor(it.descriptor) }
                val irParent = findIrParent(firDeclaration)!!
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irDeclaration = createIrConstructor(firDeclaration, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceConstructor(irDeclaration.descriptor)
            }
            else -> throw AssertionError("Should not be here: ${firDeclaration::class.java}: ${firDeclaration.render()}")
        }
    }

    fun getIrPropertyOrFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return irSymbolTable.referenceProperty(it.descriptor) }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irProperty = createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceProperty(irProperty.descriptor)
            }
            is FirField -> {
                fieldCache[fir]?.let { return irSymbolTable.referenceField(it.descriptor) }
                val irField = createIrField(fir).apply {
                    setAndModifyParent(findIrParent(fir))
                }
                irSymbolTable.referenceField(irField.descriptor)
            }
            else -> throw IllegalArgumentException("Unexpected fir in property symbol: ${fir.render()}")
        }
    }

    fun getIrBackingFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return irSymbolTable.referenceField(it.backingField!!.descriptor) }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irProperty = createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                irSymbolTable.referenceField(irProperty.backingField!!.descriptor)
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable<*>): IrVariableSymbol {
        val irDeclaration = localStorage.getVariable(firVariable)
            ?: throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
        return irSymbolTable.referenceVariable(irDeclaration.descriptor)
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                val containingFile = firProvider.getFirCallableContainerFile(firVariableSymbol)
                val parentClassSymbol = firVariableSymbol.callableId.classId?.let { firSymbolProvider.getClassLikeSymbolByFqName(it) }
                val irParentClass = (parentClassSymbol?.fir as? FirClass<*>)?.let { getIrClass(it) }
                val irEnumEntry = getIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    origin = if (containingFile == null) IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB else IrDeclarationOrigin.DEFINED
                )
                irSymbolTable.referenceEnumEntry(irEnumEntry.descriptor)
            }
            is FirValueParameter -> {
                val irDeclaration = localStorage.getParameter(firDeclaration)
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
                irSymbolTable.referenceValueParameter(irDeclaration.descriptor)
            }
            else -> {
                getIrVariableSymbol(firDeclaration)
            }
        }
    }
}