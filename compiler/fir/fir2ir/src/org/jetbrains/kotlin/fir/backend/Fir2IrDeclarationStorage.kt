/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor
) : Fir2IrComponents by components {
    private val firSymbolProvider = session.firSymbolProvider

    private val firProvider = session.firProvider

    private val fragmentCache = mutableMapOf<FqName, IrExternalPackageFragment>()

    private val fileCache = mutableMapOf<FirFile, IrFile>()

    private val functionCache = mutableMapOf<FirFunction<*>, IrSimpleFunction>()

    private val constructorCache = mutableMapOf<FirConstructor, IrConstructor>()

    private val initializerCache = mutableMapOf<FirAnonymousInitializer, IrAnonymousInitializer>()

    private val propertyCache = mutableMapOf<FirProperty, IrProperty>()

    private val fieldCache = mutableMapOf<FirField, IrField>()

    private val localStorage = Fir2IrLocalStorage()

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

    fun enterScope(descriptor: DeclarationDescriptor) {
        symbolTable.enterScope(descriptor)
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
            descriptor is WrappedPropertyDescriptor ||
            descriptor is WrappedEnumEntryDescriptor
        ) {
            localStorage.leaveCallable()
        }
        symbolTable.leaveScope(descriptor)
    }

    private fun FirTypeRef.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun ConeKotlinType.toIrType(typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeContext) }

    private fun getIrExternalPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return fragmentCache.getOrPut(fqName) {
            // TODO: module descriptor is wrong here
            return symbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
        }
    }

    internal fun addDeclarationsToExternalClass(regularClass: FirRegularClass, irClass: IrClass) {
        if (regularClass.symbol.classId.packageFqName.startsWith(Name.identifier("kotlin"))) {
            // Note: yet this is necessary only for *Range / *Progression classes
            // due to BE optimizations (for lowering) that use their first / last / step members
            // TODO: think how to refactor this piece of code and/or merge it with similar Fir2IrVisitor fragment
            val processedNames = mutableSetOf<Name>()
            for (declaration in regularClass.declarations) {
                irClass.declarations += when (declaration) {
                    is FirSimpleFunction -> {
                        processedNames += declaration.name
                        createIrFunction(declaration, irClass)
                    }
                    is FirProperty -> {
                        processedNames += declaration.name
                        createIrProperty(declaration, irClass)
                    }
                    is FirConstructor -> {
                        createIrConstructor(declaration, irClass)
                    }
                    is FirRegularClass -> {
                        classifierStorage.createIrClass(declaration, irClass)
                    }
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
                            irClass.declarations += createIrFunction(fakeOverrideSymbol.fir, irClass)
                        }
                    }
                    scope.processPropertiesByName(name) { propertySymbol ->
                        if (propertySymbol is FirPropertySymbol) {
                            val fakeOverrideSymbol =
                                FirClassSubstitutionScope.createFakeOverrideProperty(session, propertySymbol.fir, propertySymbol)
                            irClass.declarations += createIrProperty(fakeOverrideSymbol.fir, irClass)
                        }
                    }
                }
            }
            for (irDeclaration in irClass.declarations) {
                irDeclaration.parent = irClass
            }
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
        if (function is FirSimpleFunction) {
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
        if (function !is FirConstructor) {
            val thisOrigin = IrDeclarationOrigin.DEFINED
            val receiverTypeRef = if (function !is FirPropertyAccessor) function?.receiverTypeRef else parentPropertyReceiverType
            with(classifierStorage) {
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

    fun createIrFunction(
        function: FirFunction<*>,
        irParent: IrDeclarationParent?,
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
        classifierStorage.preCacheTypeParameters(function)
        val name = simpleFunction?.name
            ?: if (isLambda) Name.special("<anonymous>") else Name.special("<no name provided>")
        val visibility = simpleFunction?.visibility ?: Visibilities.LOCAL
        val created = function.convertWithOffsets { startOffset, endOffset ->
            enterScope(descriptor)
            val result = symbolTable.declareSimpleFunction(startOffset, endOffset, origin, descriptor) { symbol ->
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

        leaveScope(created.descriptor)
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

    fun createIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrConstructor {
        val descriptor = WrappedClassConstructorDescriptor()
        val isPrimary = constructor.isPrimary
        val visibility = when (irParent.modality) {
            Modality.SEALED -> Visibilities.PRIVATE
            else -> constructor.visibility
        }
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareConstructor(startOffset, endOffset, origin, descriptor) { symbol ->
                IrConstructorImpl(
                    startOffset, endOffset, origin, symbol,
                    Name.special("<init>"), visibility,
                    constructor.returnTypeRef.toIrType(),
                    isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = false
                ).apply {
                    enterScope(descriptor)
                }.bindAndDeclareParameters(constructor, descriptor, irParent, isStatic = true).apply {
                    leaveScope(descriptor)
                }
            }
        }
        constructorCache[constructor] = created
        return created
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
        return symbolTable.declareSimpleFunction(
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

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    ): IrProperty {
        val containerSource = property.containerSource
        val descriptor = containerSource?.let { WrappedPropertyDescriptorWithContainerSource(it) } ?: WrappedPropertyDescriptor()
        classifierStorage.preCacheTypeParameters(property)
        return property.convertWithOffsets { startOffset, endOffset ->
            enterScope(descriptor)
            val result = symbolTable.declareProperty(
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
            leaveScope(descriptor)
            propertyCache[property] = result
            result
        }
    }

    fun getCachedIrProperty(property: FirProperty): IrProperty? = propertyCache[property]

    private fun createIrField(field: FirField): IrField {
        val descriptor = WrappedFieldDescriptor()
        val origin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
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
                    isStatic = field.isStatic,
                    isFakeOverride = false
                ).apply {
                    descriptor.bind(this)
                    fieldCache[field] = this
                }
            }
        }
    }

    private fun createIrParameter(
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
        val irVariable = variable.convertWithOffsets { startOffset, endOffset ->
            declareIrVariable(
                startOffset, endOffset, origin,
                variable.name, type, variable.isVar, isConst = false, isLateinit = false
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
        getCachedIrConstructor(firConstructor)?.let { return symbolTable.referenceConstructor(it.descriptor) }
        val irParent = findIrParent(firConstructor) as IrClass
        val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
        val irDeclaration = createIrConstructor(firConstructor, irParent, origin = parentOrigin).apply {
            setAndModifyParent(irParent)
        }
        return symbolTable.referenceConstructor(irDeclaration.descriptor)
    }

    fun getIrFunctionSymbol(firFunctionSymbol: FirFunctionSymbol<*>): IrFunctionSymbol {
        return when (val firDeclaration = firFunctionSymbol.fir) {
            is FirSimpleFunction, is FirAnonymousFunction -> {
                getCachedIrFunction(firDeclaration)?.let { return symbolTable.referenceSimpleFunction(it.descriptor) }
                val irParent = findIrParent(firDeclaration)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irDeclaration = createIrFunction(firDeclaration, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                symbolTable.referenceSimpleFunction(irDeclaration.descriptor)
            }
            is FirConstructor -> {
                getIrConstructorSymbol(firDeclaration.symbol)
            }
            else -> throw AssertionError("Should not be here: ${firDeclaration::class.java}: ${firDeclaration.render()}")
        }
    }

    fun getIrPropertyOrFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return symbolTable.referenceProperty(it.descriptor) }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irProperty = createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                symbolTable.referenceProperty(irProperty.descriptor)
            }
            is FirField -> {
                fieldCache[fir]?.let { return symbolTable.referenceField(it.descriptor) }
                val irField = createIrField(fir).apply {
                    setAndModifyParent(findIrParent(fir))
                }
                symbolTable.referenceField(irField.descriptor)
            }
            else -> throw IllegalArgumentException("Unexpected fir in property symbol: ${fir.render()}")
        }
    }

    fun getIrBackingFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val fir = firVariableSymbol.fir) {
            is FirProperty -> {
                propertyCache[fir]?.let { return symbolTable.referenceField(it.backingField!!.descriptor) }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val irProperty = createIrProperty(fir, irParent, origin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }
                symbolTable.referenceField(irProperty.backingField!!.descriptor)
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable<*>): IrVariableSymbol {
        val irDeclaration = localStorage.getVariable(firVariable)
            ?: throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
        return symbolTable.referenceVariable(irDeclaration.descriptor)
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                classifierStorage.getCachedIrEnumEntry(firDeclaration)?.let { return symbolTable.referenceEnumEntry(it.descriptor) }
                val containingFile = firProvider.getFirCallableContainerFile(firVariableSymbol)
                val parentClassSymbol = firVariableSymbol.callableId.classId?.let { firSymbolProvider.getClassLikeSymbolByFqName(it) }
                val irParentClass = (parentClassSymbol?.fir as? FirClass<*>)?.let { classifierStorage.getCachedIrClass(it) }
                val irEnumEntry = classifierStorage.createIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    origin = if (containingFile == null) IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB else IrDeclarationOrigin.DEFINED
                )
                symbolTable.referenceEnumEntry(irEnumEntry.descriptor)
            }
            is FirValueParameter -> {
                val irDeclaration = localStorage.getParameter(firDeclaration)
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
                symbolTable.referenceValueParameter(irDeclaration.descriptor)
            }
            else -> {
                getIrVariableSymbol(firDeclaration)
            }
        }
    }
}