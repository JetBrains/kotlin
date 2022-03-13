/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.types.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.isKFunctionInvoke
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor
) : Fir2IrComponents by components {

    private val firProvider = session.firProvider

    private val fragmentCache = ConcurrentHashMap<FqName, IrExternalPackageFragment>()

    private val builtInsFragmentCache = ConcurrentHashMap<FqName, IrExternalPackageFragment>()

    private val fileCache = ConcurrentHashMap<FirFile, IrFile>()

    private val functionCache = ConcurrentHashMap<FirFunction, IrSimpleFunction>()

    private val constructorCache = ConcurrentHashMap<FirConstructor, IrConstructor>()

    private val initializerCache = ConcurrentHashMap<FirAnonymousInitializer, IrAnonymousInitializer>()

    private val propertyCache = ConcurrentHashMap<FirProperty, IrProperty>()

    // interface A { /* $1 */ fun foo() }
    // interface B : A {
    //      /* $2 */ fake_override fun foo()
    // }
    // interface C : B {
    //    /* $3 */ override fun foo()
    // }
    //
    // We've got FIR declarations only for $1 and $3, but we've got a fake override for $2 in IR
    // and just to simplify things we create a synthetic FIR for $2, while it can't be referenced from other FIR nodes.
    //
    // But when we binding overrides for $3, we want it had $2 ad it's overridden,
    // so remember that in class B there's a fake override $2 for real $1.
    //
    // Thus we may obtain it by fakeOverridesInClass[ir(B)][fir(A::foo)] -> fir(B::foo)
    private val fakeOverridesInClass = mutableMapOf<IrClass, MutableMap<FirCallableDeclaration, FirCallableDeclaration>>()

    // For pure fields (from Java) only
    private val fieldToPropertyCache = ConcurrentHashMap<Pair<FirField, IrDeclarationParent>, IrProperty>()

    private val delegatedReverseCache = ConcurrentHashMap<IrDeclaration, FirDeclaration>()

    private val fieldCache = ConcurrentHashMap<FirField, IrField>()

    private val localStorage by threadLocal { Fir2IrLocalStorage() }

    private fun areCompatible(firFunction: FirFunction, irFunction: IrFunction): Boolean {
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
                else -> {}
            }
        }
        val scope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
        scope.getCallableNames().forEach { callableName ->
            buildList {
                fakeOverrideGenerator.generateFakeOverridesForName(
                    irClass, scope, callableName, firClass, this, realDeclarationSymbols = emptySet()
                )
            }.also(fakeOverrideGenerator::bindOverriddenSymbols)
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

    private fun getIrExternalOrBuiltInsPackageFragment(fqName: FqName, firOrigin: FirDeclarationOrigin): IrExternalPackageFragment {
        val isBuiltIn = fqName in BUILT_INS_PACKAGE_FQ_NAMES
        return if (isBuiltIn) getIrBuiltInsPackageFragment(fqName) else getIrExternalPackageFragment(fqName, firOrigin)
    }

    private fun getIrBuiltInsPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return builtInsFragmentCache.getOrPut(fqName) {
            return symbolTable.declareExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
        }
    }

    fun getIrExternalPackageFragment(
        fqName: FqName,
        firOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): IrExternalPackageFragment {
        return fragmentCache.getOrPut(fqName) {
            // Make sure that external package fragments have a different module descriptor. The module descriptors are compared
            // to determine if objects need regeneration because they are from different modules.
            // But keep original module descriptor for the fragments coming from parts compiled on the previous incremental step
            val externalFragmentModuleDescriptor =
                if (firOrigin == FirDeclarationOrigin.Precompiled) moduleDescriptor
                else FirModuleDescriptor(session)
            return symbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, externalFragmentModuleDescriptor))
        }
    }

    private fun findIrClass(lookupTag: ConeClassLikeLookupTag): IrClass? =
        if (lookupTag.classId.isLocal) {
            classifierStorage.getCachedLocalClass(lookupTag)
        } else {
            val firSymbol = lookupTag.toSymbol(session)
            if (firSymbol is FirClassSymbol) {
                classifierStorage.getIrClassSymbol(firSymbol).owner
            } else {
                null
            }
        }

    internal fun findIrParent(
        packageFqName: FqName,
        parentLookupTag: ConeClassLikeLookupTag?,
        firBasedSymbol: FirBasedSymbol<*>,
        firOrigin: FirDeclarationOrigin
    ): IrDeclarationParent? {
        return if (parentLookupTag != null) {
            findIrClass(parentLookupTag)
        } else {
            val containerFile = when (firBasedSymbol) {
                is FirCallableSymbol -> firProvider.getFirCallableContainerFile(firBasedSymbol)
                is FirClassLikeSymbol -> firProvider.getFirClassifierContainerFileIfAny(firBasedSymbol)
                else -> error("Unknown symbol: $firBasedSymbol")
            }

            when {
                containerFile != null -> fileCache[containerFile]
                firBasedSymbol is FirCallableSymbol -> getIrExternalPackageFragment(packageFqName, firOrigin)
                // TODO: All classes from BUILT_INS_PACKAGE_FQ_NAMES are considered built-ins now,
                // which is not exact and can lead to some problems
                else -> getIrExternalOrBuiltInsPackageFragment(packageFqName, firOrigin)
            }
        }
    }

    internal fun findIrParent(callableDeclaration: FirCallableDeclaration): IrDeclarationParent? {
        val firBasedSymbol = callableDeclaration.symbol
        val callableId = firBasedSymbol.callableId
        val callableOrigin = callableDeclaration.origin
        return findIrParent(callableId.packageName, callableDeclaration.containingClass(), firBasedSymbol, callableOrigin)
    }

    private fun IrDeclaration.setAndModifyParent(irParent: IrDeclarationParent?) {
        if (irParent != null) {
            parent = irParent
            if (irParent is IrExternalPackageFragment) {
                irParent.declarations += this
            }
        }
    }

    private fun <T : IrFunction> T.declareDefaultSetterParameter(type: IrType): T {
        valueParameters = listOf(
            createDefaultSetterParameter(startOffset, endOffset, type, parent = this)
        )
        return this
    }

    internal fun createDefaultSetterParameter(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        parent: IrFunction
    ): IrValueParameter {
        return irFactory.createValueParameter(
            startOffset, endOffset, IrDeclarationOrigin.DEFINED, IrValueParameterSymbolImpl(),
            Name.special("<set-?>"), 0, type,
            varargElementType = null,
            isCrossinline = false, isNoinline = false,
            isHidden = false, isAssignable = false
        ).apply {
            this.parent = parent
        }
    }

    private fun <T : IrFunction> T.declareParameters(
        function: FirFunction?,
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
                    typeContext,
                    skipDefaultParameter = isFakeOverride
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
                        val name = (function as? FirAnonymousFunction)?.label?.name?.let {
                            val suffix = it.takeIf(Name::isValidIdentifier) ?: "\$receiver"
                            Name.identifier("\$this\$$suffix")
                        } ?: SpecialNames.THIS
                        declareThisReceiverParameter(
                            symbolTable,
                            thisType = receiverTypeRef.toIrType(typeContext),
                            thisOrigin = thisOrigin,
                            startOffset = startOffset,
                            endOffset = endOffset,
                            name = name
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
        function: FirFunction?,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        isStatic: Boolean,
        parentPropertyReceiverType: FirTypeRef? = null
    ): T {
        setAndModifyParent(irParent)
        declareParameters(function, thisReceiverOwner, isStatic, parentPropertyReceiverType)
        return this
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters)) {
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    fun getCachedIrFunction(function: FirFunction): IrSimpleFunction? =
        if (function is FirSimpleFunction) getCachedIrFunction(function)
        else localStorage.getLocalFunction(function)

    fun getCachedIrFunction(function: FirSimpleFunction): IrSimpleFunction? {
        return if (function.visibility == Visibilities.Local) {
            localStorage.getLocalFunction(function)
        } else {
            functionCache[function]
        }
    }

    fun getCachedIrFunction(
        function: FirSimpleFunction,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrSimpleFunction? {
        if (function.visibility == Visibilities.Local) {
            return localStorage.getLocalFunction(function)
        }
        return getCachedIrCallable(function, dispatchReceiverLookupTag, functionCache, signatureCalculator) { signature ->
            symbolTable.referenceSimpleFunctionIfAny(signature)?.owner
        }
    }

    internal fun cacheDelegationFunction(function: FirSimpleFunction, irFunction: IrSimpleFunction) {
        functionCache[function] = irFunction
        delegatedReverseCache[irFunction] = function
    }

    fun originalDeclarationForDelegated(irDeclaration: IrDeclaration): FirDeclaration? = delegatedReverseCache[irDeclaration]

    internal fun declareIrSimpleFunction(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction =
        if (signature == null) {
            factory(IrSimpleFunctionSymbolImpl())
        } else {
            symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)
        }

    fun getOrCreateIrFunction(
        function: FirSimpleFunction,
        irParent: IrDeclarationParent?,
        isLocal: Boolean = false,
    ): IrSimpleFunction {
        getCachedIrFunction(function)?.let { return it }
        return createIrFunction(function, irParent, isLocal = isLocal)
    }

    fun createIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        containingClass: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunction = convertCatching(function) {
        val simpleFunction = function as? FirSimpleFunction
        val isLambda = function.source?.elementType == KtNodeTypes.FUNCTION_LITERAL
        val updatedOrigin = when {
            isLambda -> IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            function.symbol.callableId.isKFunctionInvoke() -> IrDeclarationOrigin.FAKE_OVERRIDE
            simpleFunction?.isStatic == true && simpleFunction.name in ENUM_SYNTHETIC_NAMES -> IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER

            // Kotlin built-in class and Java originated method (Collection.forEach, etc.)
            // It's necessary to understand that such methods do not belong to DefaultImpls but actually generated as default
            // See org.jetbrains.kotlin.backend.jvm.lower.InheritedDefaultMethodsOnClassesLoweringKt.isDefinitelyNotDefaultImplsMethod
            (irParent as? IrClass)?.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                    function.origin == FirDeclarationOrigin.Enhancement -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            else -> function.computeIrOrigin(predefinedOrigin)
        }
        val signature = if (isLocal) null else signatureComposer.composeSignature(function, containingClass)
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy function
            return createIrLazyFunction(function as FirSimpleFunction, signature, irParent, updatedOrigin)
        }
        classifierStorage.preCacheTypeParameters(function)
        val name = simpleFunction?.name
            ?: if (isLambda) SpecialNames.ANONYMOUS else Name.special("<no name provided>")
        val visibility = simpleFunction?.visibility ?: Visibilities.Local
        val isSuspend =
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.type?.isSuspendFunctionType(session) == true
            else simpleFunction?.isSuspend == true
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
                    convertAnnotationsForNonDeclaredMembers(function, origin)
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
            (function.symbol.originalForSubstitutionOverride as? FirNamedFunctionSymbol)?.let {
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
    ): IrAnonymousInitializer = convertCatching(anonymousInitializer) {
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareAnonymousInitializer(startOffset, endOffset, IrDeclarationOrigin.DEFINED, irParent.descriptor).apply {
                this.parent = irParent
                initializerCache[anonymousInitializer] = this
            }
        }
    }

    fun getCachedIrConstructor(
        constructor: FirConstructor,
        signatureCalculator: () -> IdSignature? = { null }
    ): IrConstructor? {
        return constructorCache[constructor] ?: signatureCalculator()?.let { signature ->
            symbolTable.referenceConstructorIfAny(signature)?.let { irConstructorSymbol ->
                val irConstructor = irConstructorSymbol.owner
                constructorCache[constructor] = irConstructor
                irConstructor
            }
        }
    }

    private fun declareIrConstructor(signature: IdSignature?, factory: (IrConstructorSymbol) -> IrConstructor): IrConstructor =
        if (signature == null)
            factory(IrConstructorSymbolImpl())
        else
            symbolTable.declareConstructor(signature, { Fir2IrConstructorSymbol(signature) }, factory)


    fun createIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false
    ): IrConstructor = convertCatching(constructor) {
        val origin = constructor.computeIrOrigin(predefinedOrigin)
        val isPrimary = constructor.isPrimary
        classifierStorage.preCacheTypeParameters(constructor)
        val signature = if (isLocal) null else signatureComposer.composeSignature(constructor)
        val created = constructor.convertWithOffsets { startOffset, endOffset ->
            declareIrConstructor(signature) { symbol ->
                irFactory.createConstructor(
                    startOffset, endOffset, origin, symbol,
                    SpecialNames.INIT, components.visibilityConverter.convertToDescriptorVisibility(constructor.visibility),
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

    fun getOrCreateIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false
    ): IrConstructor {
        getCachedIrConstructor(constructor)?.let { return it }
        return createIrConstructor(constructor, irParent, predefinedOrigin, isLocal)
    }

    private fun declareIrAccessor(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction =
        if (signature == null)
            factory(IrSimpleFunctionSymbolImpl())
        else
            symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature, containerSource) }, factory)

    private fun createIrPropertyAccessor(
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
        propertyAccessorForAnnotations: FirPropertyAccessor? = propertyAccessor,
    ): IrSimpleFunction = convertCatching(propertyAccessor ?: property) {
        val prefix = if (isSetter) "set" else "get"
        val signature = if (isLocal) null else signatureComposer.composeAccessorSignature(property, isSetter, containingClass)
        val containerSource = (correspondingProperty as? IrProperty)?.containerSource
        return declareIrAccessor(
            signature,
            containerSource
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
                    convertAnnotationsForNonDeclaredMembers(propertyAccessor, origin)
                }

                if (propertyAccessorForAnnotations != null) {
                    convertAnnotationsForNonDeclaredMembers(propertyAccessorForAnnotations, origin)
                }
                with(classifierStorage) {
                    setTypeParameters(
                        property, ConversionTypeContext(
                            definitelyNotNull = false,
                            origin = if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
                        )
                    )
                }
                // NB: we should enter accessor' scope before declaring its parameters
                // (both setter default and receiver ones, if any)
                enterScope(this)
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeContext.DEFAULT.inSetter())
                    )
                }
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
                        correspondingProperty.containingClass, !isSetter, session, scopeSession, declarationStorage, fakeOverrideGenerator
                    )
                }
            }
        }
    }

    internal fun IrProperty.createBackingField(
        property: FirProperty,
        origin: IrDeclarationOrigin,
        visibility: DescriptorVisibility,
        name: Name,
        isFinal: Boolean,
        firInitializerExpression: FirExpression?,
        type: IrType? = null
    ): IrField = convertCatching(property) {
        val inferredType = type ?: firInitializerExpression!!.typeRef.toIrType()
        return declareIrField(null) { symbol ->
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
                convertAnnotationsForNonDeclaredMembers(property, origin)
            }
        }
    }

    private val FirProperty.fieldVisibility: Visibility
        get() = when {
            hasExplicitBackingField -> backingField?.visibility ?: status.visibility
            isLateInit -> setter?.visibility ?: status.visibility
            isConst -> status.visibility
            hasJvmFieldAnnotation -> status.visibility
            else -> Visibilities.Private
        }

    private fun declareIrProperty(
        signature: IdSignature?,
        containerSource: DeserializedContainerSource?,
        factory: (IrPropertySymbol) -> IrProperty
    ): IrProperty =
        if (signature == null)
            factory(IrPropertySymbolImpl())
        else
            symbolTable.declareProperty(signature, { Fir2IrPropertySymbol(signature, containerSource) }, factory)

    private fun declareIrField(signature: IdSignature?, factory: (IrFieldSymbol) -> IrField): IrField =
        if (signature == null)
            factory(IrFieldSymbolImpl())
        else
            symbolTable.declareField(signature, { IrFieldPublicSymbolImpl(signature) }, factory)

    fun getOrCreateIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        isLocal: Boolean = false,
    ): IrProperty {
        getCachedIrProperty(property)?.let { return it }
        return createIrProperty(property, irParent, isLocal = isLocal)
    }

    fun getOrCreateIrPropertyByPureField(
        field: FirField,
        irParent: IrDeclarationParent
    ): IrProperty {
        return fieldToPropertyCache.getOrPut(field to irParent) {
            val containingClassId = (irParent as? IrClass)?.classId
            createIrProperty(
                field.toStubProperty(),
                irParent,
                containingClass = containingClassId?.let { ConeClassLikeLookupTagImpl(it) }
            )
        }
    }

    private fun FirField.toStubProperty(): FirProperty {
        val field = this
        return buildProperty {
            source = field.source
            moduleData = field.moduleData
            origin = field.origin
            returnTypeRef = field.returnTypeRef
            name = field.name
            isVar = field.isVar
            getter = field.getter
            setter = field.setter
            symbol = FirPropertySymbol(field.symbol.callableId)
            isLocal = false
            status = field.status
        }
    }

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        containingClass: ConeClassLikeLookupTag? = null,
    ): IrProperty = convertCatching(property) {
        val origin = property.computeIrOrigin(predefinedOrigin)
        val signature = if (isLocal) null else signatureComposer.composeSignature(property, containingClass)
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy property
            return createIrLazyProperty(property, signature, irParent, origin)
        }
        classifierStorage.preCacheTypeParameters(property)
        if (property.delegate != null) {
            ((property.delegate as? FirQualifiedAccess)?.calleeReference?.resolvedSymbol?.fir as? FirTypeParameterRefsOwner)?.let {
                classifierStorage.preCacheTypeParameters(it)
            }
        }
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
                    convertAnnotationsForNonDeclaredMembers(property, origin)
                    enterScope(this)
                    if (irParent != null) {
                        parent = irParent
                    }
                    val type = property.returnTypeRef.toIrType()
                    val delegate = property.delegate
                    val getter = property.getter
                    val setter = property.setter
                    if (delegate != null || property.hasBackingField) {
                        backingField = if (delegate != null) {
                            createBackingField(
                                property, IrDeclarationOrigin.PROPERTY_DELEGATE,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                Name.identifier("${property.name}\$delegate"), true, delegate
                            )
                        } else {
                            val initializer = property.backingField?.initializer ?: property.initializer
                            // There are cases when we get here for properties
                            // that have no backing field. For example, in the
                            // funExpression.kt test there's an attempt
                            // to access the `javaClass` property of the `foo0`'s
                            // `block` argument
                            val typeToUse = property.backingField?.returnTypeRef?.toIrType() ?: type
                            createBackingField(
                                property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                property.name, property.isVal, initializer, typeToUse
                            ).also { field ->
                                if (initializer is FirConstExpression<*>) {
                                    // TODO: Normally we shouldn't have error type here
                                    val constType = initializer.typeRef.toIrType().takeIf { it !is IrErrorType } ?: typeToUse
                                    field.initializer = factory.createExpressionBody(initializer.toIrConst(constType))
                                }
                            }
                        }
                    }
                    if (irParent != null) {
                        backingField?.parent = irParent
                    }
                    this.getter = createIrPropertyAccessor(
                        getter, property, this, type, irParent, thisReceiverOwner, false,
                        when {
                            origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> origin
                            delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                            getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                            else -> origin
                        },
                        startOffset, endOffset, isLocal, containingClass,
                        property.unwrapFakeOverrides().getter,
                    )
                    if (property.isVar) {
                        this.setter = createIrPropertyAccessor(
                            setter, property, this, type, irParent, thisReceiverOwner, true,
                            when {
                                delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset, isLocal, containingClass,
                            property.unwrapFakeOverrides().setter,
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

    fun getCachedIrProperty(
        property: FirProperty,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrProperty? {
        return getCachedIrCallable(property, dispatchReceiverLookupTag, propertyCache, signatureCalculator) { signature ->
            symbolTable.referencePropertyIfAny(signature)?.owner
        }
    }

    private inline fun <reified FC : FirCallableDeclaration, reified IC : IrDeclaration> getCachedIrCallable(
        declaration: FC,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
        cache: MutableMap<FC, IC>,
        signatureCalculator: () -> IdSignature?,
        referenceIfAny: (IdSignature) -> IC?
    ): IC? {
        val isFakeOverride = dispatchReceiverLookupTag != null && dispatchReceiverLookupTag != declaration.containingClass()
        if (!isFakeOverride) {
            cache[declaration]?.let { return it }
        }
        return signatureCalculator()?.let { signature ->
            referenceIfAny(signature)?.let { irDeclaration ->
                if (!isFakeOverride) {
                    cache[declaration] = irDeclaration
                }
                irDeclaration
            }
        }
    }

    internal fun cacheDelegatedProperty(property: FirProperty, irProperty: IrProperty) {
        propertyCache[property] = irProperty
        delegatedReverseCache[irProperty] = property
    }

    internal fun saveFakeOverrideInClass(
        irClass: IrClass,
        originalDeclaration: FirCallableDeclaration,
        fakeOverride: FirCallableDeclaration
    ) {
        fakeOverridesInClass.getOrPut(irClass, ::mutableMapOf)[originalDeclaration] = fakeOverride
    }

    fun getFakeOverrideInClass(
        irClass: IrClass,
        callableDeclaration: FirCallableDeclaration
    ): FirCallableDeclaration? {
        if (irClass is Fir2IrLazyClass) {
            irClass.getFakeOverridesByName(callableDeclaration.symbol.callableId.callableName)
        }
        return fakeOverridesInClass[irClass]?.get(callableDeclaration)
    }

    fun getCachedIrField(field: FirField): IrField? = fieldCache[field]

    fun createIrFieldAndDelegatedMembers(field: FirField, owner: FirClass, irClass: IrClass): IrField? {
        // Either take a corresponding constructor property backing field,
        // or create a separate delegate field
        val irField = getOrCreateDelegateIrField(field, owner, irClass)
        delegatedMemberGenerator.generate(irField, field, owner, irClass)
        if (owner.isLocalClassOrAnonymousObject()) {
            delegatedMemberGenerator.generateBodies()
        }
        // If it's a property backing field, it should not be added to the class in Fir2IrConverter, so it's not returned
        return irField.takeIf { it.correspondingPropertySymbol == null }
    }

    private fun getOrCreateDelegateIrField(field: FirField, owner: FirClass, irClass: IrClass): IrField {
        val initializer = field.initializer
        if (initializer is FirQualifiedAccessExpression && initializer.explicitReceiver == null) {
            val resolvedSymbol = initializer.calleeReference.resolvedSymbol as? FirValueParameterSymbol
            if (resolvedSymbol is FirValueParameterSymbol) {
                val name = resolvedSymbol.name
                val constructorProperty = owner.declarations.filterIsInstance<FirProperty>().find {
                    it.name == name && it.source?.kind is KtFakeSourceElementKind.PropertyFromParameter
                }
                if (constructorProperty != null) {
                    val irProperty = getOrCreateIrProperty(constructorProperty, irClass)
                    val backingField = irProperty.backingField!!
                    fieldCache[field] = backingField
                    return backingField
                }
            }
        }
        val irField = createIrField(
            field,
            typeRef = initializer?.typeRef ?: field.returnTypeRef,
            origin = IrDeclarationOrigin.DELEGATE
        )
        irField.setAndModifyParent(irClass)
        return irField
    }

    private fun createIrField(
        field: FirField,
        typeRef: FirTypeRef = field.returnTypeRef,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField = convertCatching(field) {
        val type = typeRef.toIrType()
        return field.convertWithOffsets { startOffset, endOffset ->
            irFactory.createField(
                startOffset, endOffset, origin, IrFieldSymbolImpl(),
                field.name, type, components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                isFinal = field.modality == Modality.FINAL,
                isExternal = false,
                isStatic = field.isStatic
            ).apply {
                fieldCache[field] = this
                val initializer = field.initializer
                if (initializer is FirConstExpression<*>) {
                    this.initializer = factory.createExpressionBody(initializer.toIrConst(type))
                }
            }
        }
    }

    internal fun createIrParameter(
        valueParameter: FirValueParameter,
        index: Int = UNDEFINED_PARAMETER_INDEX,
        useStubForDefaultValueStub: Boolean = true,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
        skipDefaultParameter: Boolean = false,
    ): IrValueParameter = convertCatching(valueParameter) {
        val origin = valueParameter.computeIrOrigin()
        val type = valueParameter.returnTypeRef.toIrType()
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            irFactory.createValueParameter(
                startOffset, endOffset, origin, IrValueParameterSymbolImpl(),
                valueParameter.name, index, type,
                if (!valueParameter.isVararg) null
                else valueParameter.returnTypeRef.coneType.arrayElementType()?.toIrType(typeContext),
                isCrossinline = valueParameter.isCrossinline, isNoinline = valueParameter.isNoinline,
                isHidden = false, isAssignable = false
            ).apply {
                if (!skipDefaultParameter && valueParameter.defaultValue.let {
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
    ): IrVariable =
        IrVariableImpl(
            startOffset, endOffset, origin, IrVariableSymbolImpl(), name, type,
            isVar, isConst, isLateinit
        )

    fun createIrVariable(
        variable: FirVariable,
        irParent: IrDeclarationParent,
        givenOrigin: IrDeclarationOrigin? = null
    ): IrVariable = convertCatching(variable) {
        val type = ((variable.initializer as? FirComponentCall)?.typeRef ?: variable.returnTypeRef).toIrType()
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
        localStorage.putVariable(variable, irVariable)
        return irVariable
    }

    fun createIrLocalDelegatedProperty(
        property: FirProperty,
        irParent: IrDeclarationParent
    ): IrLocalDelegatedProperty = convertCatching(property) {
        val type = property.returnTypeRef.toIrType()
        val origin = IrDeclarationOrigin.DEFINED
        val irProperty = property.convertWithOffsets { startOffset, endOffset ->
            irFactory.createLocalDelegatedProperty(
                startOffset,
                endOffset,
                origin,
                IrLocalDelegatedPropertySymbolImpl(),
                property.name,
                type,
                property.isVar
            )
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
        val fir = firConstructorSymbol.fir
        return getIrCallableSymbol(
            firConstructorSymbol,
            dispatchReceiverLookupTag = null,
            getCachedIrDeclaration = { constructor: FirConstructor, _, calculator -> getCachedIrConstructor(constructor, calculator) },
            createIrDeclaration = { parent, origin -> createIrConstructor(fir, parent as IrClass, predefinedOrigin = origin) },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                val symbol = Fir2IrConstructorSymbol(signature)
                val irConstructor = fir.convertWithOffsets { startOffset, endOffset ->
                    symbolTable.declareConstructor(signature, { symbol }) {
                        Fir2IrLazyConstructor(
                            components, startOffset, endOffset, declarationOrigin, fir, symbol
                        ).apply {
                            parent = lazyParent
                        }
                    }
                }
                constructorCache[fir] = irConstructor
                // NB: this is needed to prevent recursions in case of self bounds
                (irConstructor as Fir2IrLazyConstructor).prepareTypeParameters()
                irConstructor
            }
        ) as IrConstructorSymbol
    }

    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag? = null
    ): IrFunctionSymbol {
        return when (val fir = firFunctionSymbol.fir) {
            is FirAnonymousFunction -> {
                getCachedIrFunction(fir)?.let { return it.symbol }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val declarationOrigin = computeDeclarationOrigin(firFunctionSymbol, parentOrigin)
                createIrFunction(fir, irParent, predefinedOrigin = declarationOrigin).symbol
            }
            is FirSimpleFunction -> {
                val unmatchedReceiver = dispatchReceiverLookupTag != firFunctionSymbol.containingClass()
                if (unmatchedReceiver) {
                    generateLazyFakeOverrides(fir.name, dispatchReceiverLookupTag)
                }
                val originalSymbol = getIrCallableSymbol(
                    firFunctionSymbol,
                    dispatchReceiverLookupTag,
                    getCachedIrDeclaration = ::getCachedIrFunction,
                    createIrDeclaration = { parent, origin ->
                        createIrFunction(fir, parent, predefinedOrigin = origin)
                    },
                    createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                        createIrLazyFunction(fir, signature, lazyParent, declarationOrigin)
                    }
                ) as IrFunctionSymbol
                if (unmatchedReceiver && dispatchReceiverLookupTag is ConeClassLookupTagWithFixedSymbol) {
                    val originalFunction = originalSymbol.owner as IrSimpleFunction
                    dispatchReceiverLookupTag.findIrFakeOverride(fir.name, originalFunction) as IrFunctionSymbol
                } else {
                    originalSymbol
                }
            }
            is FirConstructor -> {
                getIrConstructorSymbol(fir.symbol)
            }
            else -> error("Unknown kind of function: ${fir::class.java}: ${fir.render()}")
        }
    }

    private fun createIrLazyFunction(
        fir: FirSimpleFunction,
        signature: IdSignature,
        lazyParent: Fir2IrLazyClass,
        declarationOrigin: IrDeclarationOrigin
    ): IrSimpleFunction {
        val symbol = Fir2IrSimpleFunctionSymbol(signature, fir.containerSource)
        val firFunctionSymbol = fir.symbol
        val irFunction = fir.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareSimpleFunction(signature, { symbol }) {
                val isFakeOverride = fir.isSubstitutionOrIntersectionOverride &&
                        firFunctionSymbol.dispatchReceiverClassOrNull() !=
                        firFunctionSymbol.originalForSubstitutionOverride?.dispatchReceiverClassOrNull()
                Fir2IrLazySimpleFunction(
                    components, startOffset, endOffset, declarationOrigin,
                    fir, lazyParent.fir, symbol, isFakeOverride
                ).apply {
                    this.parent = lazyParent
                }
            }
        }
        functionCache[fir] = irFunction
        // NB: this is needed to prevent recursions in case of self bounds
        (irFunction as Fir2IrLazySimpleFunction).prepareTypeParameters()
        return irFunction
    }

    fun getIrPropertySymbol(
        firPropertySymbol: FirPropertySymbol,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag? = null
    ): IrSymbol {
        val fir = firPropertySymbol.fir
        if (fir.isLocal) {
            return localStorage.getDelegatedProperty(fir)?.symbol ?: getIrVariableSymbol(fir)
        }
        val containingClassLookupTag = firPropertySymbol.containingClass()
        val unmatchedReceiver = dispatchReceiverLookupTag != containingClassLookupTag
        if (unmatchedReceiver) {
            generateLazyFakeOverrides(fir.name, dispatchReceiverLookupTag)
        }

        fun ConeClassLikeLookupTag?.getIrCallableSymbol() = getIrCallableSymbol(
            firPropertySymbol,
            dispatchReceiverLookupTag = this,
            getCachedIrDeclaration = ::getCachedIrProperty,
            createIrDeclaration = { parent, origin -> createIrProperty(fir, parent, predefinedOrigin = origin) },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                createIrLazyProperty(fir, signature, lazyParent, declarationOrigin)
            }
        )

        val originalSymbol = dispatchReceiverLookupTag.getIrCallableSymbol()
        val originalProperty = originalSymbol.owner as IrProperty

        fun IrProperty.isIllegalFakeOverride(): Boolean {
            if (!isFakeOverride) return false
            val overriddenSymbols = overriddenSymbols
            if (overriddenSymbols.isEmpty() || overriddenSymbols.any { it.owner.isIllegalFakeOverride() }) {
                return true
            }
            return false
        }

        if (dispatchReceiverLookupTag != null &&
            firPropertySymbol is FirSyntheticPropertySymbol &&
            originalProperty.isIllegalFakeOverride()
        ) {
            // Fallback for a synthetic property complex case
            return containingClassLookupTag.getIrCallableSymbol()
        }

        return if (unmatchedReceiver && dispatchReceiverLookupTag is ConeClassLookupTagWithFixedSymbol) {
            dispatchReceiverLookupTag.findIrFakeOverride(fir.name, originalProperty) as IrPropertySymbol
        } else {
            originalSymbol
        }
    }

    private fun createIrLazyProperty(
        fir: FirProperty,
        signature: IdSignature,
        lazyParent: Fir2IrLazyClass,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val symbol = Fir2IrPropertySymbol(signature, fir.containerSource)
        val firPropertySymbol = fir.symbol
        val irProperty = fir.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareProperty(signature, { symbol }) {
                val isFakeOverride =
                    fir.isSubstitutionOrIntersectionOverride &&
                            firPropertySymbol.dispatchReceiverClassOrNull() !=
                            firPropertySymbol.originalForSubstitutionOverride?.dispatchReceiverClassOrNull()
                Fir2IrLazyProperty(
                    components, startOffset, endOffset, declarationOrigin, fir, lazyParent.fir, symbol, isFakeOverride
                ).apply {
                    this.parent = lazyParent
                }
            }
        }
        propertyCache[fir] = irProperty
        // NB: this is needed to prevent recursions in case of self bounds
        (irProperty as Fir2IrLazyProperty).prepareTypeParameters()
        return irProperty
    }

    private inline fun <reified S : IrSymbol, reified D : IrOverridableDeclaration<S>> ConeClassLookupTagWithFixedSymbol.findIrFakeOverride(
        name: Name, originalDeclaration: IrOverridableDeclaration<S>
    ): IrSymbol? {
        val dispatchReceiverIrClass =
            classifierStorage.getIrClassSymbol(toSymbol(session) as FirClassSymbol).owner
        return dispatchReceiverIrClass.declarations.find {
            it is D && it.isFakeOverride && it.name == name && it.overrides(originalDeclaration)
        }?.symbol
    }

    private fun generateLazyFakeOverrides(name: Name, dispatchReceiverLookupTag: ConeClassLikeLookupTag?) {
        val firClassSymbol = dispatchReceiverLookupTag?.toSymbol(session) as? FirClassSymbol
        if (firClassSymbol != null) {
            val irClass = classifierStorage.getIrClassSymbol(firClassSymbol).owner
            if (irClass is Fir2IrLazyClass) {
                irClass.getFakeOverridesByName(name)
            }
        }
    }

    private inline fun <
            reified FS : FirCallableSymbol<*>,
            reified F : FirCallableDeclaration,
            I : IrSymbolOwner,
            > getIrCallableSymbol(
        firSymbol: FS,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
        getCachedIrDeclaration: (firDeclaration: F, dispatchReceiverLookupTag: ConeClassLikeLookupTag?, () -> IdSignature?) -> I?,
        createIrDeclaration: (parent: IrDeclarationParent?, origin: IrDeclarationOrigin) -> I,
        createIrLazyDeclaration: (signature: IdSignature, lazyOwner: Fir2IrLazyClass, origin: IrDeclarationOrigin) -> I,
    ): IrSymbol {
        val fir = firSymbol.fir as F
        val irParent by lazy { findIrParent(fir) }
        val signature by lazy { signatureComposer.composeSignature(fir, dispatchReceiverLookupTag) }
        synchronized(symbolTable.lock) {
            getCachedIrDeclaration(fir, dispatchReceiverLookupTag.takeIf { it !is ConeClassLookupTagWithFixedSymbol }) {
                // Parent calculation provokes declaration calculation for some members from IrBuiltIns
                @Suppress("UNUSED_EXPRESSION") irParent
                signature
            }?.let { return it.symbol }
            val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
            val declarationOrigin = computeDeclarationOrigin(firSymbol, parentOrigin)
            // TODO: package fragment members (?)
            when (val parent = irParent) {
                is Fir2IrLazyClass -> {
                    assert(parentOrigin != IrDeclarationOrigin.DEFINED) {
                        "Should not have reference to public API uncached property from source code"
                    }
                    signature?.let {
                        return createIrLazyDeclaration(it, parent, declarationOrigin).symbol
                    }
                }
                is IrLazyClass -> {
                    val unwrapped = fir.unwrapFakeOverrides()
                    if (unwrapped !== fir) {
                        when (unwrapped) {
                            is FirSimpleFunction -> {
                                return getIrFunctionSymbol(unwrapped.symbol)
                            }
                            is FirProperty -> {
                                return getIrPropertySymbol(unwrapped.symbol)
                            }
                        }
                    }
                }
            }
            return createIrDeclaration(irParent, declarationOrigin).apply {
                (this as IrDeclaration).setAndModifyParent(irParent)
            }.symbol
        }
    }

    private fun computeDeclarationOrigin(
        symbol: FirCallableSymbol<*>,
        parentOrigin: IrDeclarationOrigin
    ): IrDeclarationOrigin = when {
        symbol.fir.isIntersectionOverride || symbol.fir.isSubstitutionOverride -> IrDeclarationOrigin.FAKE_OVERRIDE
        parentOrigin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                (symbol.fir.origin is FirDeclarationOrigin.Enhancement || symbol.fir.origin is FirDeclarationOrigin.Java) -> {
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        }
        symbol.origin is FirDeclarationOrigin.Plugin -> IrPluginDeclarationOrigin((symbol.origin as FirDeclarationOrigin.Plugin).key)
        else -> parentOrigin
    }

    fun getIrFieldSymbol(firFieldSymbol: FirFieldSymbol): IrSymbol {
        val fir = firFieldSymbol.fir
        val irProperty = fieldCache[fir] ?: run {
            // In case of type parameters from the parent as the field's return type, find the parent ahead to cache type parameters.
            val irParent = findIrParent(fir)
            createIrField(fir).apply {
                setAndModifyParent(irParent)
            }
        }
        return irProperty.symbol
    }

    fun getIrBackingFieldSymbol(firBackingFieldSymbol: FirBackingFieldSymbol): IrSymbol {
        return getIrPropertyForwardedSymbol(firBackingFieldSymbol.fir.propertySymbol.fir)
    }

    fun getIrDelegateFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return getIrPropertyForwardedSymbol(firVariableSymbol.fir)
    }

    private fun getIrPropertyForwardedSymbol(fir: FirVariable): IrSymbol {
        return when (fir) {
            is FirProperty -> {
                if (fir.isLocal) {
                    return localStorage.getDelegatedProperty(fir)?.delegate?.symbol ?: getIrVariableSymbol(fir)
                }
                propertyCache[fir]?.let { return it.backingField!!.symbol }
                val irParent = findIrParent(fir)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                createIrProperty(fir, irParent, predefinedOrigin = parentOrigin).apply {
                    setAndModifyParent(irParent)
                }.backingField!!.symbol
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable): IrVariableSymbol {
        return localStorage.getVariable(firVariable)?.symbol
            ?: run {
                throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
            }
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                classifierStorage.getCachedIrEnumEntry(firDeclaration)?.let { return it.symbol }
                val containingFile = firProvider.getFirCallableContainerFile(firVariableSymbol)
                val irParentClass = firDeclaration.containingClass()?.let { findIrClass(it) }
                classifierStorage.createIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    predefinedOrigin = if (containingFile != null) IrDeclarationOrigin.DEFINED else
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

    private fun IrMutableAnnotationContainer.convertAnnotationsForNonDeclaredMembers(
        firAnnotationContainer: FirAnnotationContainer, origin: IrDeclarationOrigin,
    ) {
        if ((firAnnotationContainer as? FirDeclaration)?.let { it.isFromLibrary || it.isPrecompiled } == true
            || origin == IrDeclarationOrigin.FAKE_OVERRIDE
        ) {
            annotationGenerator.generate(this, firAnnotationContainer)
        }
    }

    companion object {
        internal val ENUM_SYNTHETIC_NAMES = mapOf(
            Name.identifier("values") to IrSyntheticBodyKind.ENUM_VALUES,
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF
        )
    }

    private inline fun <R> convertCatching(element: FirElement, block: () -> R): R {
        try {
            return block()
        } catch (e: Throwable) {
            throw KotlinExceptionWithAttachments("Exception was thrown during transformation of ${element.render()}", e)
        }
    }
}
