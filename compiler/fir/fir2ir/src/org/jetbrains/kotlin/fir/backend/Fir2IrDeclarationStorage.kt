/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtFakeSourceElementKind
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
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.impl.IrScriptImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.impl.SCRIPT_K2_ORIGIN
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
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage
) : Fir2IrComponents by components {

    private val firProvider = session.firProvider

    private val fragmentCache: ConcurrentHashMap<FqName, IrExternalPackageFragment> = ConcurrentHashMap()

    private val builtInsFragmentCache: ConcurrentHashMap<FqName, IrExternalPackageFragment> = ConcurrentHashMap()

    private val fileCache: ConcurrentHashMap<FirFile, IrFile> = ConcurrentHashMap()

    private val scriptCache: ConcurrentHashMap<FirScript, IrScript> = ConcurrentHashMap()

    private val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunction> = commonMemberStorage.functionCache

    private val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructor> = commonMemberStorage.constructorCache

    private val initializerCache: ConcurrentHashMap<FirAnonymousInitializer, IrAnonymousInitializer> = ConcurrentHashMap()

    private val propertyCache: ConcurrentHashMap<FirProperty, IrProperty> = commonMemberStorage.propertyCache

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
    // But when we're binding overrides for $3, we want it had $2 ad it's overridden,
    // so remember that in class B there's a fake override $2 for real $1.
    //
    // Thus, we may obtain it by fakeOverridesInClass[ir(B)][fir(A::foo)] -> fir(B::foo)
    //
    // Note: reusing is necessary here, because sometimes (see testFakeOverridesInPlatformModule)
    // we have to match fake override in platform class with overridden fake overrides in common class
    private val fakeOverridesInClass: MutableMap<IrClass, MutableMap<FakeOverrideKey, FirCallableDeclaration>> =
        commonMemberStorage.fakeOverridesInClass

    sealed class FakeOverrideKey {
        data class Signature(val signature: IdSignature) : FakeOverrideKey()

        /*
         * Used for declarations which don't have id signature (e.g. members of local classes)
         */
        data class Declaration(val declaration: FirCallableDeclaration) : FakeOverrideKey()
    }

    private fun FirCallableDeclaration.asFakeOverrideKey(): FakeOverrideKey {
        return when (val signature = signatureComposer.composeSignature(this)) {
            null -> FakeOverrideKey.Declaration(this)
            else -> FakeOverrideKey.Signature(signature)
        }
    }

    // For pure fields (from Java) only
    private val fieldToPropertyCache: ConcurrentHashMap<Pair<FirField, IrDeclarationParent>, IrProperty> = ConcurrentHashMap()

    private val delegatedReverseCache: ConcurrentHashMap<IrDeclaration, FirDeclaration> = ConcurrentHashMap()

    private val fieldCache: ConcurrentHashMap<FirField, IrField> = ConcurrentHashMap()

    private data class FieldStaticOverrideKey(val lookupTag: ConeClassLikeLookupTag, val name: Name)

    private val fieldStaticOverrideCache: ConcurrentHashMap<FieldStaticOverrideKey, IrField> = ConcurrentHashMap()

    private val localStorage: Fir2IrLocalCallableStorage by threadLocal { Fir2IrLocalCallableStorage() }

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
                            is IrScriptSymbol -> {
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
        val scope = firClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
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
            declaration is IrEnumEntry ||
            declaration is IrScript
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(declaration: IrDeclaration) {
        if (declaration is IrSimpleFunction ||
            declaration is IrConstructor ||
            declaration is IrAnonymousInitializer ||
            declaration is IrProperty ||
            declaration is IrEnumEntry ||
            declaration is IrScript
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
            symbolTable.declareExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
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
                else FirModuleDescriptor(session, moduleDescriptor.builtIns)
            symbolTable.declareExternalPackageFragment(FirPackageFragmentDescriptor(fqName, externalFragmentModuleDescriptor))
        }
    }

    internal fun findIrParent(
        packageFqName: FqName,
        parentLookupTag: ConeClassLikeLookupTag?,
        firBasedSymbol: FirBasedSymbol<*>,
        firOrigin: FirDeclarationOrigin
    ): IrDeclarationParent? {
        return if (parentLookupTag != null) {
            classifierStorage.findIrClass(parentLookupTag)
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
        return findIrParent(callableId.packageName, callableDeclaration.containingClassLookupTag(), firBasedSymbol, callableOrigin)
    }

    private fun IrDeclaration.setAndModifyParent(irParent: IrDeclarationParent?) {
        if (irParent != null) {
            parent = irParent
            if (irParent is IrExternalPackageFragment) {
                irParent.declarations += this
            }
        }
    }

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
            startOffset, endOffset, IrDeclarationOrigin.DEFINED, IrValueParameterSymbolImpl(),
            name ?: SpecialNames.IMPLICIT_SET_PARAMETER, parent.contextReceiverParametersCount, type,
            varargElementType = null,
            isCrossinline = isCrossinline, isNoinline = isNoinline,
            isHidden = false, isAssignable = false
        ).apply {
            this.parent = parent
            if (firValueParameter != null) {
                annotationGenerator.generate(this, firValueParameter)
            }
        }
    }

    private fun <T : IrFunction> T.declareParameters(
        function: FirFunction?,
        containingClass: IrClass?,
        isStatic: Boolean,
        forSetter: Boolean,
        // Can be not-null only for property accessors
        parentPropertyReceiver: FirReceiverParameter?
    ) {
        val parent = this
        if (function is FirSimpleFunction || function is FirConstructor) {
            with(classifierStorage) {
                setTypeParameters(function)
            }
        }
        val typeContext = if (forSetter) ConversionTypeContext.IN_SETTER else ConversionTypeContext.DEFAULT
        if (function is FirDefaultPropertySetter) {
            val valueParameter = function.valueParameters.first()
            val type = valueParameter.returnTypeRef.toIrType(ConversionTypeContext.IN_SETTER)
            declareDefaultSetterParameter(type, valueParameter)
        } else if (function != null) {
            val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

            contextReceiverParametersCount = contextReceivers.size
            valueParameters = buildList {
                addContextReceiverParametersTo(contextReceivers, parent, this)

                function.valueParameters.mapIndexedTo(this) { index, valueParameter ->
                    createIrParameter(
                        valueParameter, index + contextReceiverParametersCount,
                        useStubForDefaultValueStub = function !is FirConstructor || containingClass?.name != Name.identifier("Enum"),
                        typeContext,
                        skipDefaultParameter = isFakeOverride || origin == IrDeclarationOrigin.DELEGATED_MEMBER
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
                        thisType = receiver.typeRef.toIrType(typeContext),
                        thisOrigin = thisOrigin,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        name = name,
                        explicitReceiver = receiver,
                    )
                }
            }
            // See [LocalDeclarationsLowering]: "local function must not have dispatch receiver."
            val isLocal = function is FirSimpleFunction && function.isLocal
            if (function !is FirAnonymousFunction && containingClass != null && !isStatic && !isLocal) {
                dispatchReceiverParameter = declareThisReceiverParameter(
                    thisType = containingClass.thisReceiver?.type ?: error("No this receiver"),
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

    private fun <T : IrFunction> T.bindAndDeclareParameters(
        function: FirFunction?,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        isStatic: Boolean,
        forSetter: Boolean,
        parentPropertyReceiver: FirReceiverParameter? = null
    ): T {
        setAndModifyParent(irParent)
        declareParameters(function, thisReceiverOwner, isStatic, forSetter, parentPropertyReceiver)
        return this
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters.drop(contextReceivers.size))) {
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
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrSimpleFunction? {
        if (function.visibility == Visibilities.Local) {
            return localStorage.getLocalFunction(function)
        }
        return getCachedIrCallable(function, fakeOverrideOwnerLookupTag, functionCache, signatureCalculator) { signature ->
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
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction =
        if (signature == null) {
            factory(IrSimpleFunctionSymbolImpl())
        } else {
            symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature) }, factory)
        }

    fun getOrCreateIrFunction(
        function: FirSimpleFunction,
        irParent: IrDeclarationParent?,
        isLocal: Boolean = false,
        forceTopLevelPrivate: Boolean = false,
    ): IrSimpleFunction {
        getCachedIrFunction(function)?.let { return it }
        return createIrFunction(
            function,
            irParent,
            fakeOverrideOwnerLookupTag = function.containingClassLookupTag(),
            isLocal = isLocal,
            forceTopLevelPrivate = forceTopLevelPrivate
        )
    }

    fun createIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        forceTopLevelPrivate: Boolean = false,
    ): IrSimpleFunction = convertCatching(function) {
        val simpleFunction = function as? FirSimpleFunction
        val isLambda = function is FirAnonymousFunction && function.isLambda
        val updatedOrigin = when {
            isLambda -> IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            function.symbol.callableId.isKFunctionInvoke() -> IrDeclarationOrigin.FAKE_OVERRIDE
            simpleFunction?.isStatic == true && simpleFunction.name in ENUM_SYNTHETIC_NAMES -> IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER

            // Kotlin built-in class and Java originated method (Collection.forEach, etc.)
            // It's necessary to understand that such methods do not belong to DefaultImpls but actually generated as default
            // See org.jetbrains.kotlin.backend.jvm.lower.InheritedDefaultMethodsOnClassesLoweringKt.isDefinitelyNotDefaultImplsMethod
            (irParent as? IrClass)?.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB &&
                    function.isJavaOrEnhancement -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            else -> function.computeIrOrigin(predefinedOrigin)
        }
        // We don't generate signatures for local classes
        // We attempt to avoid signature generation for non-local classes, with the following exceptions:
        // - special mode (generateSignatures) oriented on special backend modes
        // - lazy classes (they still use signatures)
        // - primitive types (they can be from built-ins and don't have FIR counterpart)
        // - overrides and fake overrides (sometimes we perform "receiver replacement" in FIR2IR breaking FIR->IR relation,
        // or FIR counterpart can be just created on the fly)
        val signature =
            runUnless(
                isLocal ||
                        !configuration.linkViaSignatures && irParent !is Fir2IrLazyClass &&
                        function.dispatchReceiverType?.isPrimitive != true && function.containerSource == null &&
                        updatedOrigin != IrDeclarationOrigin.FAKE_OVERRIDE && !function.isOverride
            ) {
                signatureComposer.composeSignature(function, fakeOverrideOwnerLookupTag, forceTopLevelPrivate)
            }
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy function
            return createIrLazyFunction(function as FirSimpleFunction, signature, irParent, updatedOrigin)
        }
        val name = simpleFunction?.name
            ?: if (isLambda) SpecialNames.ANONYMOUS else Name.special("<no name provided>")
        val visibility = simpleFunction?.visibility ?: Visibilities.Local
        val isSuspend =
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.type?.isSuspendOrKSuspendFunctionType(session) == true
            else function.isSuspend
        val created = function.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrSimpleFunction(signature) { symbol ->
                classifierStorage.preCacheTypeParameters(function, symbol)
                irFactory.createFunction(
                    if (updatedOrigin == IrDeclarationOrigin.DELEGATED_MEMBER) SYNTHETIC_OFFSET else startOffset,
                    if (updatedOrigin == IrDeclarationOrigin.DELEGATED_MEMBER) SYNTHETIC_OFFSET else endOffset,
                    updatedOrigin, symbol,
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
                    enterScope(this)
                    bindAndDeclareParameters(
                        function, irParent,
                        thisReceiverOwner, isStatic = simpleFunction?.isStatic == true,
                        forSetter = false,
                    )
                    convertAnnotationsForNonDeclaredMembers(function, origin)
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
        isLocal: Boolean = false,
        forceTopLevelPrivate: Boolean = false,
    ): IrConstructor = convertCatching(constructor) {
        val origin = constructor.computeIrOrigin(predefinedOrigin)
        val isPrimary = constructor.isPrimary
        val signature =
            runUnless(isLocal || !configuration.linkViaSignatures) {
                signatureComposer.composeSignature(constructor, forceTopLevelPrivate = forceTopLevelPrivate)
            }
        val visibility = if (irParent.isAnonymousObject) Visibilities.Public else constructor.visibility
        return constructor.convertWithOffsets { startOffset, endOffset ->
            declareIrConstructor(signature) { symbol ->
                classifierStorage.preCacheTypeParameters(constructor, symbol)
                irFactory.createConstructor(
                    startOffset, endOffset, origin, symbol,
                    SpecialNames.INIT, components.visibilityConverter.convertToDescriptorVisibility(visibility),
                    constructor.returnTypeRef.toIrType(),
                    isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = constructor.isExpect
                ).apply {
                    metadata = FirMetadataSource.Function(constructor)
                    // Add to cache before generating parameters to prevent an infinite loop when an annotation value parameter is annotated
                    // with the annotation itself.
                    constructorCache[constructor] = this
                    enterScope(this)
                    bindAndDeclareParameters(constructor, irParent, isStatic = false, forSetter = false)
                    leaveScope(this)
                }
            }
        }
    }

    fun getOrCreateIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        forceTopLevelPrivate: Boolean = false,
    ): IrConstructor {
        getCachedIrConstructor(constructor)?.let { return it }
        return createIrConstructor(constructor, irParent, predefinedOrigin, isLocal, forceTopLevelPrivate = forceTopLevelPrivate)
    }

    private fun declareIrAccessor(
        signature: IdSignature?,
        factory: (IrSimpleFunctionSymbol) -> IrSimpleFunction
    ): IrSimpleFunction =
        if (signature == null)
            factory(IrSimpleFunctionSymbolImpl())
        else
            symbolTable.declareSimpleFunction(signature, { Fir2IrSimpleFunctionSymbol(signature) }, factory)

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
        dontUseSignature: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        propertyAccessorForAnnotations: FirPropertyAccessor? = propertyAccessor,
        forceTopLevelPrivate: Boolean = false,
    ): IrSimpleFunction = convertCatching(propertyAccessor ?: property) {
        val prefix = if (isSetter) "set" else "get"
        val signature =
            runUnless(dontUseSignature) {
                signatureComposer.composeAccessorSignature(property, isSetter, fakeOverrideOwnerLookupTag, forceTopLevelPrivate)
            }
        val containerSource = (correspondingProperty as? IrProperty)?.containerSource
        return declareIrAccessor(
            signature
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
                        property, if (isSetter) ConversionTypeContext.IN_SETTER else ConversionTypeContext.DEFAULT
                    )
                }
                // NB: we should enter accessor' scope before declaring its parameters
                // (both setter default and receiver ones, if any)
                enterScope(this)
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeContext.IN_SETTER),
                        firValueParameter = null
                    )
                }
                bindAndDeclareParameters(
                    propertyAccessor, irParent,
                    thisReceiverOwner, isStatic = irParent !is IrClass || propertyAccessor?.isStatic == true,
                    forSetter = isSetter,
                    parentPropertyReceiver = property.receiverParameter,
                )
                leaveScope(this)
                if (irParent != null) {
                    parent = irParent
                }
                if (correspondingProperty is Fir2IrLazyProperty && correspondingProperty.containingClass != null && !isFakeOverride && thisReceiverOwner != null) {
                    this.overriddenSymbols = correspondingProperty.fir.generateOverriddenAccessorSymbols(
                        correspondingProperty.containingClass, !isSetter
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
        return declareIrField { symbol ->
            irFactory.createField(
                startOffset, endOffset, origin, symbol,
                name, inferredType,
                visibility, isFinal = isFinal,
                isExternal = property.isExternal,
                isStatic = property.isStatic || !(parent is IrClass || parent is IrScript),
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
            hasJvmFieldAnnotation(session) -> status.visibility
            else -> Visibilities.Private
        }

    private fun declareIrProperty(
        signature: IdSignature?,
        factory: (IrPropertySymbol) -> IrProperty
    ): IrProperty =
        if (signature == null)
            factory(IrPropertySymbolImpl())
        else
            symbolTable.declareProperty(signature, { Fir2IrPropertySymbol(signature) }, factory)

    private fun declareIrField(factory: (IrFieldSymbol) -> IrField): IrField =
        factory(IrFieldSymbolImpl())

    fun getOrCreateIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        isLocal: Boolean = false,
        forceTopLevelPrivate: Boolean = false,
    ): IrProperty {
        getCachedIrProperty(property)?.let { return it }
        return createIrProperty(property, irParent, isLocal = isLocal, forceTopLevelPrivate = forceTopLevelPrivate)
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
                fakeOverrideOwnerLookupTag = containingClassId?.toLookupTag()
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
        }.apply {
            isStubPropertyForPureField = true
        }
    }

    private object IsStubPropertyForPureFieldKey : FirDeclarationDataKey()

    private var FirProperty.isStubPropertyForPureField: Boolean? by FirDeclarationDataRegistry.data(IsStubPropertyForPureFieldKey)

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        thisReceiverOwner: IrClass? = irParent as? IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = (irParent as? IrClass)?.classId?.toLookupTag(),
        forceTopLevelPrivate: Boolean = false,
    ): IrProperty = convertCatching(property) {
        val origin =
            if (property.isStatic && property.name in ENUM_SYNTHETIC_NAMES) IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER
            else property.computeIrOrigin(predefinedOrigin)
        // See similar comments in createIrFunction above
        val signature =
            runUnless(
                isLocal ||
                        !configuration.linkViaSignatures && irParent !is Fir2IrLazyClass &&
                        property.dispatchReceiverType?.isPrimitive != true && property.containerSource == null &&
                        origin != IrDeclarationOrigin.FAKE_OVERRIDE && !property.isOverride
            ) {
                signatureComposer.composeSignature(property, fakeOverrideOwnerLookupTag, forceTopLevelPrivate)
            }
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy property
            return createIrLazyProperty(property, signature, irParent, origin)
        }
        return property.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrProperty(signature) { symbol ->
                classifierStorage.preCacheTypeParameters(property, symbol)
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
                            ((delegate as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedBaseSymbol()?.fir as? FirTypeParameterRefsOwner)?.let {
                                classifierStorage.preCacheTypeParameters(it, symbol)
                            }
                            createBackingField(
                                property, IrDeclarationOrigin.PROPERTY_DELEGATE,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                NameUtils.propertyDelegateName(property.name), true, delegate
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
                            origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER -> origin
                            delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                            origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                            origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                            getter is FirDefaultPropertyGetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                            else -> origin
                        },
                        startOffset, endOffset,
                        dontUseSignature = signature == null, fakeOverrideOwnerLookupTag,
                        property.unwrapFakeOverrides().getter,
                        forceTopLevelPrivate = forceTopLevelPrivate,
                    )
                    if (property.isVar) {
                        this.setter = createIrPropertyAccessor(
                            setter, property, this, type, irParent, thisReceiverOwner, true,
                            when {
                                delegate != null -> IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR
                                origin == IrDeclarationOrigin.FAKE_OVERRIDE -> origin
                                origin == IrDeclarationOrigin.DELEGATED_MEMBER -> origin
                                setter is FirDefaultPropertySetter -> IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
                                else -> origin
                            },
                            startOffset, endOffset,
                            dontUseSignature = signature == null, fakeOverrideOwnerLookupTag,
                            property.unwrapFakeOverrides().setter,
                            forceTopLevelPrivate = forceTopLevelPrivate,
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
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrProperty? {
        return getCachedIrCallable(property, fakeOverrideOwnerLookupTag, propertyCache, signatureCalculator) { signature ->
            symbolTable.referencePropertyIfAny(signature)?.owner
        }
    }

    private inline fun <reified FC : FirCallableDeclaration, reified IC : IrDeclaration> getCachedIrCallable(
        declaration: FC,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        cache: MutableMap<FC, IC>,
        signatureCalculator: () -> IdSignature?,
        referenceIfAny: (IdSignature) -> IC?
    ): IC? {
        val isFakeOverride = fakeOverrideOwnerLookupTag != null && fakeOverrideOwnerLookupTag != declaration.containingClassLookupTag()
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
        fakeOverridesInClass.getOrPut(irClass, ::mutableMapOf)[originalDeclaration.asFakeOverrideKey()] = fakeOverride
    }

    fun getFakeOverrideInClass(
        irClass: IrClass,
        callableDeclaration: FirCallableDeclaration
    ): FirCallableDeclaration? {
        if (irClass is Fir2IrLazyClass) {
            irClass.getFakeOverridesByName(callableDeclaration.symbol.callableId.callableName)
        }
        val map = fakeOverridesInClass[irClass]
        return map?.get(callableDeclaration.asFakeOverrideKey())
    }

    fun getCachedIrDelegateOrBackingField(field: FirField): IrField? = fieldCache[field]

    fun getCachedIrFieldStaticFakeOverrideByDeclaration(field: FirField): IrField? {
        val ownerLookupTag = field.containingClassLookupTag() ?: return null
        return fieldStaticOverrideCache[FieldStaticOverrideKey(ownerLookupTag, field.name)]
    }

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
            val resolvedSymbol = initializer.calleeReference.toResolvedValueParameterSymbol()
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
        return createIrField(
            field,
            irParent = irClass,
            typeRef = initializer?.typeRef ?: field.returnTypeRef,
            origin = IrDeclarationOrigin.DELEGATE
        )
    }

    internal fun createIrField(
        field: FirField,
        irParent: IrDeclarationParent?,
        typeRef: FirTypeRef = field.returnTypeRef,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField = convertCatching(field) {
        val type = typeRef.toIrType()
        val classId = (irParent as? IrClass)?.classId
        val containingClassLookupTag = classId?.toLookupTag()
        val signature = signatureComposer.composeSignature(field, containingClassLookupTag)
        return field.convertWithOffsets { startOffset, endOffset ->
            if (signature != null) {
                symbolTable.declareField(
                    signature, symbolFactory = { IrFieldPublicSymbolImpl(signature) }
                ) { symbol ->
                    irFactory.createField(
                        startOffset, endOffset, origin, symbol,
                        field.name, type, components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                        isFinal = field.modality == Modality.FINAL,
                        isExternal = false,
                        isStatic = field.isStatic
                    )
                }
            } else {
                irFactory.createField(
                    startOffset, endOffset, origin, IrFieldSymbolImpl(),
                    field.name, type, components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                    isFinal = field.modality == Modality.FINAL,
                    isExternal = false,
                    isStatic = field.isStatic
                )
            }.apply {
                val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(field, containingClassLookupTag)
                if (staticFakeOverrideKey == null) {
                    fieldCache[field] = this
                } else {
                    fieldStaticOverrideCache[staticFakeOverrideKey] = this
                }
                val initializer = field.unwrapFakeOverrides().initializer
                if (initializer is FirConstExpression<*>) {
                    this.initializer = factory.createExpressionBody(initializer.toIrConst(type))
                }
                setAndModifyParent(irParent)
            }
        }
    }

    // This function returns null if this field/ownerClassId combination does not describe static fake override
    private fun getFieldStaticFakeOverrideKey(field: FirField, ownerLookupTag: ConeClassLikeLookupTag?): FieldStaticOverrideKey? {
        if (ownerLookupTag == null || !field.isStatic ||
            !field.isSubstitutionOrIntersectionOverride && ownerLookupTag == field.containingClassLookupTag()
        ) return null
        return FieldStaticOverrideKey(ownerLookupTag, field.name)
    }

    internal fun createIrParameter(
        valueParameter: FirValueParameter,
        index: Int = UNDEFINED_PARAMETER_INDEX,
        useStubForDefaultValueStub: Boolean = true,
        typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
        skipDefaultParameter: Boolean = false,
    ): IrValueParameter = convertCatching(valueParameter) {
        val origin = valueParameter.computeIrOrigin()
        val type = valueParameter.returnTypeRef.toIrType(typeContext)
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
                annotationGenerator.generate(this, valueParameter)
            }
        }
        localStorage.putParameter(valueParameter, irParameter)
        return irParameter
    }

    private fun createIrParameterFromContextReceiver(
        contextReceiver: FirContextReceiver,
        index: Int,
    ): IrValueParameter = convertCatching(contextReceiver) {
        val type = contextReceiver.typeRef.toIrType()
        return contextReceiver.convertWithOffsets { startOffset, endOffset ->
            irFactory.createValueParameter(
                startOffset, endOffset, IrDeclarationOrigin.DEFINED, IrValueParameterSymbolImpl(),
                NameUtils.contextReceiverName(index), index, type,
                null,
                isCrossinline = false, isNoinline = false,
                isHidden = false, isAssignable = false
            )
        }
    }

    // TODO: KT-58686
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
        // Note: for components call, we have to change type here (to original component type) to keep compatibility with PSI2IR
        // Some backend optimizations related to withIndex() probably depend on this type: index should always be Int
        // See e.g. forInStringWithIndexWithExplicitlyTypedIndexVariable.kt from codegen box tests
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
                NameUtils.propertyDelegateName(property.name), property.delegate!!.typeRef.toIrType(),
                isVar = false, isConst = false, isLateinit = false
            )
            delegate.parent = irParent
            getter = createIrPropertyAccessor(
                property.getter, property, this, type, irParent, null, false,
                IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, dontUseSignature = true
            )
            if (property.isVar) {
                setter = createIrPropertyAccessor(
                    property.setter, property, this, type, irParent, null, true,
                    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, dontUseSignature = true
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

    fun getIrConstructorSymbol(firConstructorSymbol: FirConstructorSymbol, forceTopLevelPrivate: Boolean = false): IrConstructorSymbol {
        val fir = firConstructorSymbol.fir
        return getIrCallableSymbol(
            firConstructorSymbol,
            fakeOverrideOwnerLookupTag = null,
            getCachedIrDeclaration = { constructor: FirConstructor, _, calculator -> getCachedIrConstructor(constructor, calculator) },
            createIrDeclaration = { parent, origin ->
                createIrConstructor(fir, parent as IrClass, predefinedOrigin = origin, forceTopLevelPrivate = forceTopLevelPrivate)
            },
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
            },
            forceTopLevelPrivate = forceTopLevelPrivate
        ) as IrConstructorSymbol
    }

    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        forceTopLevelPrivate: Boolean = false,
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
                val unmatchedOwner = fakeOverrideOwnerLookupTag != firFunctionSymbol.containingClassLookupTag()
                if (unmatchedOwner) {
                    generateLazyFakeOverrides(fir.name, fakeOverrideOwnerLookupTag)
                }
                val originalSymbol = getIrCallableSymbol(
                    firFunctionSymbol,
                    fakeOverrideOwnerLookupTag,
                    getCachedIrDeclaration = ::getCachedIrFunction,
                    createIrDeclaration = { parent, origin ->
                        createIrFunction(
                            fir, parent,
                            predefinedOrigin = origin,
                            forceTopLevelPrivate = forceTopLevelPrivate,
                            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                        )
                    },
                    createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                        createIrLazyFunction(fir, signature, lazyParent, declarationOrigin)
                    },
                    forceTopLevelPrivate = forceTopLevelPrivate
                ) as IrFunctionSymbol
                if (unmatchedOwner && fakeOverrideOwnerLookupTag is ConeClassLookupTagWithFixedSymbol) {
                    val originalFunction = originalSymbol.owner as IrSimpleFunction
                    fakeOverrideOwnerLookupTag.findIrFakeOverride(fir.name, originalFunction) as IrFunctionSymbol? ?: originalSymbol
                } else {
                    originalSymbol
                }
            }
            is FirConstructor -> {
                getIrConstructorSymbol(fir.symbol, forceTopLevelPrivate = forceTopLevelPrivate)
            }
            else -> error("Unknown kind of function: ${fir::class.java}: ${fir.render()}")
        }
    }

    private fun createIrLazyFunction(
        fir: FirSimpleFunction,
        signature: IdSignature,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrSimpleFunction {
        val symbol = symbolTable.referenceSimpleFunction(signature)
        val irFunction = fir.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareSimpleFunction(signature, { symbol }) {
                val isFakeOverride = fir.isSubstitutionOrIntersectionOverride
                Fir2IrLazySimpleFunction(
                    components, startOffset, endOffset, declarationOrigin,
                    fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol, isFakeOverride
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
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        forceTopLevelPrivate: Boolean = false,
    ): IrSymbol {
        val fir = firPropertySymbol.fir
        if (fir.isLocal) {
            return localStorage.getDelegatedProperty(fir)?.symbol ?: getIrVariableSymbol(fir)
        }
        val containingClassLookupTag = firPropertySymbol.containingClassLookupTag()
        val unmatchedOwner = fakeOverrideOwnerLookupTag != containingClassLookupTag
        if (unmatchedOwner) {
            generateLazyFakeOverrides(fir.name, fakeOverrideOwnerLookupTag)
        }

        fun ConeClassLikeLookupTag?.getIrCallableSymbol() = getIrCallableSymbol(
            firPropertySymbol,
            fakeOverrideOwnerLookupTag = this,
            getCachedIrDeclaration = ::getCachedIrProperty,
            createIrDeclaration = { parent, origin ->
                createIrProperty(
                    fir, parent, predefinedOrigin = origin, forceTopLevelPrivate = forceTopLevelPrivate,
                    fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                )
            },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                createIrLazyProperty(fir, signature, lazyParent, declarationOrigin)
            },
            forceTopLevelPrivate = forceTopLevelPrivate
        )

        val originalSymbol = fakeOverrideOwnerLookupTag.getIrCallableSymbol()
        val originalProperty = originalSymbol.owner as IrProperty

        fun IrProperty.isIllegalFakeOverride(): Boolean {
            if (!isFakeOverride) return false
            val overriddenSymbols = overriddenSymbols
            return overriddenSymbols.isEmpty() || overriddenSymbols.any { it.owner.isIllegalFakeOverride() }
        }

        if (fakeOverrideOwnerLookupTag != null &&
            firPropertySymbol is FirSyntheticPropertySymbol &&
            originalProperty.isIllegalFakeOverride()
        ) {
            // Fallback for a synthetic property complex case
            return containingClassLookupTag.getIrCallableSymbol()
        }

        return if (unmatchedOwner && fakeOverrideOwnerLookupTag is ConeClassLookupTagWithFixedSymbol) {
            fakeOverrideOwnerLookupTag.findIrFakeOverride(fir.name, originalProperty) as IrPropertySymbol
        } else {
            originalSymbol
        }
    }

    private fun createIrLazyProperty(
        fir: FirProperty,
        signature: IdSignature,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val symbol = Fir2IrPropertySymbol(signature)
        val firPropertySymbol = fir.symbol

        fun create(startOffset: Int, endOffset: Int): Fir2IrLazyProperty {
            val isFakeOverride =
                fir.isSubstitutionOrIntersectionOverride &&
                        firPropertySymbol.dispatchReceiverClassLookupTagOrNull() !=
                        firPropertySymbol.originalForSubstitutionOverride?.dispatchReceiverClassLookupTagOrNull()
            return Fir2IrLazyProperty(
                components, startOffset, endOffset, declarationOrigin,
                fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol, isFakeOverride
            ).apply {
                this.parent = lazyParent
            }
        }

        val irProperty = fir.convertWithOffsets { startOffset, endOffset ->
            if (fir.isStubPropertyForPureField == true) {
                // Very special case when two similar properties can exist so conflicts in SymbolTable are possible.
                // See javaCloseFieldAndKotlinProperty.kt in BB tests
                symbolTable.declarePropertyWithSignature(signature, symbol)
                create(startOffset, endOffset)
                symbol.owner
            } else {
                symbolTable.declareProperty(signature, { symbol }) {
                    create(startOffset, endOffset)
                }
            }
        }
        propertyCache[fir] = irProperty
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

    private fun generateLazyFakeOverrides(name: Name, fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?) {
        val firClassSymbol = fakeOverrideOwnerLookupTag?.toSymbol(session) as? FirClassSymbol
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
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        getCachedIrDeclaration: (firDeclaration: F, dispatchReceiverLookupTag: ConeClassLikeLookupTag?, () -> IdSignature?) -> I?,
        createIrDeclaration: (parent: IrDeclarationParent?, origin: IrDeclarationOrigin) -> I,
        createIrLazyDeclaration: (signature: IdSignature, lazyOwner: IrDeclarationParent, origin: IrDeclarationOrigin) -> I,
        forceTopLevelPrivate: Boolean = false,
    ): IrSymbol {
        val fir = firSymbol.fir as F
        val irParent by lazy { findIrParent(fir) }
        val signature by lazy {
            signatureComposer.composeSignature(
                fir,
                fakeOverrideOwnerLookupTag,
                forceTopLevelPrivate = forceTopLevelPrivate,
                forceExpect = fakeOverrideOwnerLookupTag?.toSymbol(session)?.isExpect == true
            )
        }
        synchronized(symbolTable.lock) {
            getCachedIrDeclaration(fir, fakeOverrideOwnerLookupTag.takeIf { it !is ConeClassLookupTagWithFixedSymbol }) {
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
                is IrExternalPackageFragment -> {
                    signature?.let {
                        return createIrLazyDeclaration(it, parent, declarationOrigin).symbol
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
        parentOrigin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB && symbol.isJavaOrEnhancement -> {
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        }
        symbol.origin is FirDeclarationOrigin.Plugin -> GeneratedByPlugin((symbol.origin as FirDeclarationOrigin.Plugin).key)
        else -> parentOrigin
    }

    fun getIrFieldSymbol(
        firFieldSymbol: FirFieldSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrFieldSymbol {
        val fir = firFieldSymbol.fir
        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(fir, fakeOverrideOwnerLookupTag)
        if (staticFakeOverrideKey == null) {
            fieldCache[fir]?.let { return it.symbol }
        } else {
            generateLazyFakeOverrides(fir.name, fakeOverrideOwnerLookupTag)
            // Lazy static fake override should always exist
            return fieldStaticOverrideCache[staticFakeOverrideKey]!!.symbol
        }
        // In case of type parameters from the parent as the field's return type, find the parent ahead to cache type parameters.
        val irParent = findIrParent(fir)

        val unwrapped = fir.unwrapFakeOverrides()
        if (unwrapped !== fir) {
            return getIrFieldSymbol(unwrapped.symbol)
        }
        return createIrField(fir, irParent).symbol
    }

    fun getIrBackingFieldSymbol(firBackingFieldSymbol: FirBackingFieldSymbol): IrSymbol {
        return getIrPropertyForwardedSymbol(firBackingFieldSymbol.fir.propertySymbol.fir)
    }

    fun getIrDelegateFieldSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return getIrPropertyForwardedSymbol(firVariableSymbol.fir)
    }

    fun getCachedIrScript(script: FirScript): IrScript? = scriptCache[script]

    fun getOrCreateIrScript(script: FirScript): IrScript =
        getCachedIrScript(script) ?: script.convertWithOffsets { startOffset, endOffset ->
            val signature = signatureComposer.composeSignature(script)!!
            symbolTable.declareScript(signature, { Fir2IrScriptSymbol(signature) }) { symbol ->
                IrScriptImpl(symbol, script.name, irFactory, startOffset, endOffset).also { irScript ->
                    irScript.origin = SCRIPT_K2_ORIGIN
                    irScript.metadata = FirMetadataSource.Script(script)
                    irScript.implicitReceiversParameters = emptyList()
                    irScript.providedProperties = emptyList()
                    irScript.providedPropertiesParameters = emptyList()
                    scriptCache[script] = irScript
                }
            }
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
                val irParentClass = firDeclaration.containingClassLookupTag()?.let { classifierStorage.findIrClass(it) }
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
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF,
            Name.identifier("entries") to IrSyntheticBodyKind.ENUM_ENTRIES
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
