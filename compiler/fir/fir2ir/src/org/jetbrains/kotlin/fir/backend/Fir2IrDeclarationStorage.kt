/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
import org.jetbrains.kotlin.fir.backend.generators.FirBasedFakeOverrideGenerator
import org.jetbrains.kotlin.fir.backend.generators.isExternalParent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap

class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val sourceModuleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage
) : Fir2IrComponents by components {

    private val fragmentCache: ConcurrentHashMap<FqName, ExternalPackageFragments> = ConcurrentHashMap()
    private val moduleDescriptorCache: ConcurrentHashMap<FirModuleData, FirModuleDescriptor> = ConcurrentHashMap()

    private class ExternalPackageFragments(
        val fragmentsForDependencies: ConcurrentHashMap<FirModuleData, IrExternalPackageFragment>,
        val builtinFragmentsForDependencies: ConcurrentHashMap<FirModuleData, IrExternalPackageFragment>,
        val fragmentForPrecompiledBinaries: IrExternalPackageFragment
    )

    private val fileCache: ConcurrentHashMap<FirFile, IrFile> = ConcurrentHashMap()

    private val scriptCache: ConcurrentHashMap<FirScript, IrScript> = ConcurrentHashMap()

    private val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunctionSymbol> = commonMemberStorage.functionCache

    private val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructorSymbol> = commonMemberStorage.constructorCache

    private val initializerCache: ConcurrentHashMap<FirAnonymousInitializer, IrAnonymousInitializer> = ConcurrentHashMap()

    class PropertyCacheStorage(
        val normal: ConcurrentHashMap<FirProperty, IrPropertySymbol>,
        val synthetic: ConcurrentHashMap<FirFunction, IrPropertySymbol>
    ) {
        /**
         * Fir synthetic properties are session-dependent, so it can't be used as a cache key
         * That's why, we are using original java function as a key in that case.
         */
        private val FirSyntheticProperty.cacheKey
            get() = symbol.getterSymbol!!.delegateFunctionSymbol.fir

        operator fun set(fir: FirProperty, value: IrPropertySymbol) {
            when (fir) {
                is FirSyntheticProperty -> synthetic[fir.cacheKey] = value
                else -> normal[fir] = value
            }
        }

        operator fun get(fir: FirProperty): IrPropertySymbol? {
            return when (fir) {
                is FirSyntheticProperty -> synthetic[fir.cacheKey]
                else -> normal[fir]
            }
        }
    }
    private val propertyCache = PropertyCacheStorage(commonMemberStorage.propertyCache, commonMemberStorage.syntheticPropertyCache)
    private val getterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> =
        commonMemberStorage.getterForPropertyCache
    private val setterForPropertyCache: ConcurrentHashMap<IrSymbol, IrSimpleFunctionSymbol> =
        commonMemberStorage.setterForPropertyCache
    private val backingFieldForPropertyCache: ConcurrentHashMap<IrPropertySymbol, IrFieldSymbol> =
        commonMemberStorage.backingFieldForPropertyCache
    private val propertyForBackingFieldCache: ConcurrentHashMap<IrFieldSymbol, IrPropertySymbol> =
        commonMemberStorage.propertyForBackingFieldCache
    private val delegateVariableForPropertyCache: ConcurrentHashMap<IrLocalDelegatedPropertySymbol, IrVariableSymbol> =
        commonMemberStorage.delegateVariableForPropertyCache

    /**
     * This function is quite messy and doesn't have a good contract of what exactly is traversed.
     * The basic idea is to traverse the symbols which can be reasonably referenced from other modules.
     *
     * Be careful when using it, and avoid it, except really needed.
     */
    @DelicateDeclarationStorageApi
    fun forEachCachedDeclarationSymbol(block: (IrSymbol) -> Unit) {
        functionCache.values.forEachWithRemapping(symbolsMappingForLazyClasses::remapFunctionSymbol, block)
        constructorCache.values.forEach(block)
        propertyCache.normal.values.forEachWithRemapping(symbolsMappingForLazyClasses::remapPropertySymbol, block)
        propertyCache.synthetic.values.forEachWithRemapping(symbolsMappingForLazyClasses::remapPropertySymbol, block)
        getterForPropertyCache.values.forEachWithRemapping(symbolsMappingForLazyClasses::remapFunctionSymbol, block)
        setterForPropertyCache.values.forEachWithRemapping(symbolsMappingForLazyClasses::remapFunctionSymbol, block)
        backingFieldForPropertyCache.values.forEach(block)
        propertyForBackingFieldCache.values.forEach(block)
        delegateVariableForPropertyCache.values.forEach(block)
    }

    private inline fun <S : IrSymbol> Collection<S>.forEachWithRemapping(remapper: (S) -> S, block: (S) -> Unit) {
        for (symbol in this) {
            val updatedSymbol = if (symbol is IrFakeOverrideSymbolBase<*, *, *>) {
                remapper(symbol)
            } else {
                symbol
            }
            block(updatedSymbol)
        }
    }

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
    private val fakeOverridesInClass: MutableMap<IrClass, MutableMap<FirOverrideKey, FirCallableDeclaration>> =
        commonMemberStorage.fakeOverridesInClass

    /*
     * FIR declarations for substitution and intersection overrides, and also for delegated members are session dependent,
     *   which means that in an MPP project we can have two different functions for the same substitution overrides
     *  (in common and platform modules)
     *
     * So this cache is needed to have only one IR declaration for both overrides
     *
     * The key here is a pair of the original function (first not f/o) and lookup tag of class for which this fake override was created
     * THe value is IR function, build for this fake override during fir2ir translation of the module that contains parent class of this function
     */
    private val irForFirSessionDependantDeclarationMap: MutableMap<FakeOverrideIdentifier, IrSymbol> =
        commonMemberStorage.irForFirSessionDependantDeclarationMap

    data class FakeOverrideIdentifier(
        val originalSymbol: FirCallableSymbol<*>,
        val dispatchReceiverLookupTag: ConeClassLikeLookupTag,
        val parentIsExpect: Boolean,
    ) {
        companion object {
            context(Fir2IrComponents)
            operator fun invoke(
                originalSymbol: FirCallableSymbol<*>,
                dispatchReceiverLookupTag: ConeClassLikeLookupTag,
            ): FakeOverrideIdentifier {
                return FakeOverrideIdentifier(
                    originalSymbol,
                    dispatchReceiverLookupTag,
                    dispatchReceiverLookupTag.toFirRegularClass(session)?.isExpect == true
                )
            }
        }
    }

    sealed class FirOverrideKey {
        data class Signature(val signature: IdSignature) : FirOverrideKey()

        /*
         * Used for declarations which don't have id signature (e.g. members of local classes)
         */
        data class Declaration(val declaration: FirCallableDeclaration) : FirOverrideKey()
    }

    private fun FirCallableDeclaration.asFakeOverrideKey(): FirOverrideKey {
        return when (val signature = signatureComposer.composeSignature(this)) {
            null -> FirOverrideKey.Declaration(this)
            else -> FirOverrideKey.Signature(signature)
        }
    }

    // For pure fields (from Java) only
    private val fieldToPropertyCache: ConcurrentHashMap<Pair<FirField, IrDeclarationParent>, IrProperty> = ConcurrentHashMap()

    private val delegatedReverseCache: ConcurrentHashMap<IrSymbol, FirDeclaration> = ConcurrentHashMap()

    private val fieldCache: ConcurrentHashMap<FirField, IrFieldSymbol> = commonMemberStorage.fieldCache

    private data class FieldStaticOverrideKey(val lookupTag: ConeClassLikeLookupTag, val name: Name)

    private val fieldStaticOverrideCache: ConcurrentHashMap<FieldStaticOverrideKey, IrFieldSymbol> = ConcurrentHashMap()

    private val localStorage: Fir2IrLocalCallableStorage by threadLocal { Fir2IrLocalCallableStorage() }

    // ------------------------------------ package fragments ------------------------------------

    fun getIrExternalPackageFragment(
        fqName: FqName,
        moduleData: FirModuleData,
        firOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): IrExternalPackageFragment {
        return getIrExternalOrBuiltInsPackageFragment(fqName, moduleData, firOrigin, false)
    }

    private fun getIrExternalOrBuiltInsPackageFragment(
        fqName: FqName,
        moduleData: FirModuleData,
        firOrigin: FirDeclarationOrigin,
        allowBuiltins: Boolean
    ): IrExternalPackageFragment {
        val isBuiltIn = allowBuiltins && fqName in BUILT_INS_PACKAGE_FQ_NAMES
        val fragments = fragmentCache.getOrPut(fqName) {
            val fragmentForPrecompiledBinaries = callablesGenerator.createExternalPackageFragment(fqName, sourceModuleDescriptor)
            ExternalPackageFragments(ConcurrentHashMap(), ConcurrentHashMap(), fragmentForPrecompiledBinaries)
        }
        // Make sure that external package fragments have a different module descriptor. The module descriptors are compared
        // to determine if objects need regeneration because they are from different modules.
        // But keep the original module descriptor for the fragments coming from parts compiled on the previous incremental step
        return when (firOrigin) {
            FirDeclarationOrigin.Precompiled -> fragments.fragmentForPrecompiledBinaries
            else -> {
                val moduleDescriptor = moduleDescriptorCache.getOrPut(moduleData) {
                    FirModuleDescriptor.createDependencyModuleDescriptor(
                        moduleData,
                        sourceModuleDescriptor.builtIns
                    )
                }
                if (isBuiltIn) {
                    fragments.builtinFragmentsForDependencies.getOrPut(moduleData) {
                        callablesGenerator.createExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
                    }
                } else {
                    fragments.fragmentsForDependencies.getOrPut(moduleData) {
                        callablesGenerator.createExternalPackageFragment(fqName, moduleDescriptor)
                    }
                }
            }
        }
    }

    // ------------------------------------ files ------------------------------------

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

    @OptIn(IrImplementationDetail::class)
    internal class NonCachedSourceFileFacadeClass(
        origin: IrDeclarationOrigin,
        name: Name,
        source: SourceElement,
    ) : IrClassImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrClassSymbolImpl(), name,
        ClassKind.CLASS, DescriptorVisibilities.PUBLIC, Modality.FINAL,
        source = source
    )

    private class NonCachedSourceFacadeContainerSource(
        override val className: JvmClassName,
        override val facadeClassName: JvmClassName?
    ) : DeserializedContainerSource, FacadeClassSource {
        override val incompatibility get() = null
        override val isPreReleaseInvisible get() = false
        override val abiStability get() = DeserializedContainerAbiStability.STABLE
        override val presentableString get() = className.internalName

        override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
    }

    // ------------------------------------ functions ------------------------------------

    fun getCachedIrFunctionSymbol(
        function: FirFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunctionSymbol? {
        return if (function is FirSimpleFunction) getCachedIrFunctionSymbol(function, fakeOverrideOwnerLookupTag)
        else localStorage.getLocalFunctionSymbol(function)
    }

    fun getCachedIrFunctionSymbol(
        function: FirSimpleFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSimpleFunctionSymbol? {
        return getCachedIrFunctionSymbol(function, fakeOverrideOwnerLookupTag) {
            signatureComposer.composeSignature(function, fakeOverrideOwnerLookupTag)
        }
    }

    fun getCachedIrFunctionSymbol(
        function: FirSimpleFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrSimpleFunctionSymbol? {
        if (function.visibility == Visibilities.Local) {
            return localStorage.getLocalFunctionSymbol(function)
        }
        val cachedIrCallable = getCachedIrCallableSymbol(
            function,
            fakeOverrideOwnerLookupTag,
            functionCache::get,
            functionCache::set,
            signatureCalculator
        ) { signature ->
            symbolTable.referenceSimpleFunctionIfAny(signature)
        }
        return cachedIrCallable?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    /**
     * @param allowLazyDeclarationsCreation should be passed only during fake-override generation
     */
    fun createAndCacheIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        allowLazyDeclarationsCreation: Boolean = false
    ): IrSimpleFunction {
        val symbol = getIrFunctionSymbol(function.symbol, fakeOverrideOwnerLookupTag, isLocal) as IrSimpleFunctionSymbol
        return callablesGenerator.createIrFunction(
            function,
            irParent,
            symbol,
            predefinedOrigin,
            isLocal = isLocal,
            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
            allowLazyDeclarationsCreation
        )
    }

    internal fun createFunctionSymbol(signature: IdSignature?): IrSimpleFunctionSymbol {
        return when {
            signature != null -> symbolTable.referenceSimpleFunction(signature)
            else -> IrSimpleFunctionSymbolImpl()
        }
    }

    private fun createMemberFunctionSymbol(
        function: FirFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        parentIsExternal: Boolean
    ): IrSimpleFunctionSymbol {
        if (
            !configuration.useIrFakeOverrideBuilder ||
            parentIsExternal ||
            function !is FirSimpleFunction ||
            !function.isFakeOverride(fakeOverrideOwnerLookupTag)
        ) {
            return createFunctionSymbol(signature = null)
        }
        val containingClassSymbol = findContainingIrClassSymbol(function, fakeOverrideOwnerLookupTag)
        val originalFirFunction = function.unwrapFakeOverrides()
        val originalSymbol = getIrFunctionSymbol(originalFirFunction.symbol) as IrSimpleFunctionSymbol
        return IrFunctionFakeOverrideSymbol(originalSymbol, containingClassSymbol, idSignature = null)
    }

    private fun findContainingIrClassSymbol(
        callable: FirCallableDeclaration,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): IrClassSymbol {
        val containingClassLookupTag = when {
            fakeOverrideOwnerLookupTag != null -> fakeOverrideOwnerLookupTag
            callable.isSubstitutionOrIntersectionOverride -> callable.containingClassLookupTag()
            else -> shouldNotBeCalled()
        }
        requireNotNull(containingClassLookupTag) { "Containing class not found for ${callable.render()}"}
        return classifierStorage.findIrClass(containingClassLookupTag)?.symbol
            ?: error("IR class for $containingClassLookupTag not found")
    }

    private fun cacheIrFunctionSymbol(
        function: FirFunction,
        irFunctionSymbol: IrSimpleFunctionSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ) {
        when {
            function.visibility == Visibilities.Local || function is FirAnonymousFunction -> {
                localStorage.putLocalFunction(function, irFunctionSymbol)
            }

            function.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag) -> {
                val originalFunction = function.unwrapFakeOverridesOrDelegated()
                val key = FakeOverrideIdentifier(
                    originalFunction.symbol,
                    fakeOverrideOwnerLookupTag ?: function.containingClassLookupTag()!!
                )
                irForFirSessionDependantDeclarationMap[key] = irFunctionSymbol
            }

            else -> {
                functionCache[function] = irFunctionSymbol
            }
        }
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters.drop(contextReceivers.size))) {
            localStorage.putParameter(firParameter, irParameter.symbol)
        }
        return this
    }

    internal fun cacheDelegationFunction(function: FirSimpleFunction, irFunction: IrSimpleFunction) {
        val symbol = irFunction.symbol
        functionCache[function] = symbol
        delegatedReverseCache[symbol] = function
    }

    internal fun cacheGeneratedFunction(firFunction: FirSimpleFunction, irFunction: IrSimpleFunction) {
        functionCache[firFunction] = irFunction.symbol
    }

    // ------------------------------------ constructors ------------------------------------

    fun getCachedIrConstructorSymbol(constructor: FirConstructor): IrConstructorSymbol? {
        return constructorCache[constructor]
    }

    fun createAndCacheIrConstructor(
        constructor: FirConstructor,
        irParent: () -> IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
    ): IrConstructor {
        val symbol = getIrConstructorSymbol(constructor.symbol, isLocal)
        return callablesGenerator.createIrConstructor(
            constructor,
            irParent(),
            symbol,
            predefinedOrigin,
            allowLazyDeclarationsCreation = false
        )
    }

    private fun cacheIrConstructorSymbol(constructor: FirConstructor, irConstructorSymbol: IrConstructorSymbol) {
        constructorCache[constructor] = irConstructorSymbol
    }

    fun getIrConstructorSymbol(firConstructorSymbol: FirConstructorSymbol, isLocal: Boolean = false): IrConstructorSymbol {
        val constructor = firConstructorSymbol.fir
        getCachedIrConstructorSymbol(constructor)?.let { return it }

        // caching of created constructor is not called here, because `callablesGenerator` calls `cacheIrConstructor` by itself
        val symbol = IrConstructorSymbolImpl()
        if (!isLocal) {
            val irParent = findIrParent(constructor, fakeOverrideOwnerLookupTag = null)
            val isIntrinsicConstEvaluation =
                constructor.returnTypeRef.coneType.classId == StandardClassIds.Annotations.IntrinsicConstEvaluation
            if (irParent.isExternalParent() || isIntrinsicConstEvaluation) {
                callablesGenerator.createIrConstructor(
                    constructor,
                    irParent as IrClass,
                    symbol,
                    constructor.computeExternalOrigin(),
                    allowLazyDeclarationsCreation = true
                ).also {
                    check(it is Fir2IrLazyConstructor || isIntrinsicConstEvaluation)
                }
            }
        }
        cacheIrConstructorSymbol(constructor, symbol)

        return symbol
    }

    // ------------------------------------ properties ------------------------------------

    /**
     *    There is a difference in how FIR and IR treat synthetic properties (properties built upon java getter + optional java setter)
     *    For FIR they are really synthetic and exist only during call resolution, so FIR creates a new instance of FirSyntheticProperty
     *    each time it resolves some call to such property
     *    In IR synthetic properties are fair properties that are present in IR Java classes
     *
     *    This leads to the situation when synthetic property does not have a stable key (because FIR instance is new each time) and the only
     *    source of truth is a symbol table. To fix it (and avoid using symbol table as a storage), a pair of original getter and setter is
     *    used as a key for storage IR for synthetic properties. And to avoid introducing special cache of FirSyntheticPropertyKey -> IrProperty
     *    additional mapping level is introduced
     *
     *    - FirSyntheticPropertyKey is mapped to the first FIR synthetic property which was processed by FIR2IR
     *    - this property is mapped to IrProperty using regular propertyCache
     *
     * IMPORTANT: this whole story requires to call [prepareProperty] or [preparePropertySymbol] in the beginning of any public method
     *   which accepts arbitary FirProperty or FirPropertySymbol
     */
    private data class FirSyntheticPropertyKey(
        val originalForGetter: FirSimpleFunction,
        val originalForSetter: FirSimpleFunction?,
    ) {
        constructor(property: FirSyntheticProperty) : this(property.getter.delegate, property.setter?.delegate)
    }

    private val originalForSyntheticProperty: ConcurrentHashMap<FirSyntheticPropertyKey, FirSyntheticProperty> = ConcurrentHashMap()

    private fun prepareProperty(property: FirProperty): FirProperty {
        return when (property) {
            is FirSyntheticProperty -> originalForSyntheticProperty.getOrPut(FirSyntheticPropertyKey(property)) { property }
            else -> property
        }
    }

    fun getOrCreateIrPropertyByPureField(
        field: FirField,
        irParent: IrDeclarationParent
    ): IrProperty {
        return fieldToPropertyCache.getOrPut(field to irParent) {
            val containingClassId = (irParent as? IrClass)?.classId
            createAndCacheIrProperty(
                field.toStubProperty(),
                irParent,
                fakeOverrideOwnerLookupTag = containingClassId?.toLookupTag(),
                allowLazyDeclarationsCreation = true // pure fields exist only in java
            )
        }
    }

    fun createAndCacheIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        allowLazyDeclarationsCreation: Boolean = false
    ): IrProperty {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)

        val symbols = getIrPropertySymbols(property.symbol, fakeOverrideOwnerLookupTag)

        return callablesGenerator.createIrProperty(
            property, irParent, symbols, predefinedOrigin, fakeOverrideOwnerLookupTag, allowLazyDeclarationsCreation
        )
    }

    private fun createPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        parentIsExternal: Boolean
    ): PropertySymbols {
        if (
            configuration.useIrFakeOverrideBuilder &&
            !parentIsExternal &&
            property.isFakeOverride(fakeOverrideOwnerLookupTag)
        ) {
            return createFakeOverridePropertySymbols(property, fakeOverrideOwnerLookupTag)
        }

        val propertySymbol = IrPropertySymbolImpl()
        val getterSymbol = createFunctionSymbol(signature = null)

        val setterSymbol = runIf(property.isVar) {
            createFunctionSymbol(signature = null)
        }

        val backingFieldSymbol = runIf(property.delegate != null || property.hasBackingField) {
            createFieldSymbol()
        }

        return PropertySymbols(propertySymbol, getterSymbol, setterSymbol, backingFieldSymbol)
    }

    private fun createFakeOverridePropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): PropertySymbols {
        val originalFirProperty = property.unwrapFakeOverrides()
        val originalSymbols = getIrPropertySymbols(originalFirProperty.symbol)
        require(property.isStubPropertyForPureField != true) {
            "What are we doing here?"
        }

        val containingClassSymbol = findContainingIrClassSymbol(property, fakeOverrideOwnerLookupTag)
        val propertySymbol = IrPropertyFakeOverrideSymbol(originalSymbols.propertySymbol, containingClassSymbol, idSignature = null)
        val getterSymbol = IrFunctionFakeOverrideSymbol(originalSymbols.getterSymbol, containingClassSymbol, idSignature = null)

        val setterSymbol = runIf(property.isVar) {
            val setterIsVisible = property.setter?.let { setter ->
                (fakeOverrideOwnerLookupTag?.toSymbol(session) as? FirClassSymbol<*>)?.fir?.let { containingClass -> setter.isVisibleInClass(containingClass) }
            } ?: true
            runIf(setterIsVisible) {
                IrFunctionFakeOverrideSymbol(originalSymbols.setterSymbol!!, containingClassSymbol, idSignature = null)
            }
        }
        return PropertySymbols(propertySymbol, getterSymbol, setterSymbol, backingFieldSymbol = null)
    }

    private fun cacheIrPropertySymbols(
        property: FirProperty,
        symbols: PropertySymbols,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ) {
        val irPropertySymbol = symbols.propertySymbol
        symbols.backingFieldSymbol?.let {
            backingFieldForPropertyCache[irPropertySymbol] = it
            propertyForBackingFieldCache[it] = irPropertySymbol
        }
        symbols.getterSymbol.let {
            getterForPropertyCache[irPropertySymbol] = it
        }
        symbols.setterSymbol?.let {
            setterForPropertyCache[irPropertySymbol] = it
        }
        if (property.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)) {
            val originalProperty = property.unwrapFakeOverridesOrDelegated()
            val key = FakeOverrideIdentifier(
                originalProperty.symbol,
                fakeOverrideOwnerLookupTag ?: property.containingClassLookupTag()!!
            )
            irForFirSessionDependantDeclarationMap[key] = irPropertySymbol
        } else {
            propertyCache[property] = irPropertySymbol
        }
    }

    @Suppress("DuplicatedCode")
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

    fun getIrPropertySymbol(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSymbol {
        val property = prepareProperty(firPropertySymbol.fir)
        if (property.isLocal) {
            return localStorage.getDelegatedProperty(property) ?: getIrVariableSymbol(property)
        }
        getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag)?.let { return it }
        return getIrPropertySymbols(firPropertySymbol, fakeOverrideOwnerLookupTag).propertySymbol
    }

    private fun getIrPropertySymbols(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): PropertySymbols {
        val property = prepareProperty(firPropertySymbol.fir)
        getCachedIrPropertySymbols(property, fakeOverrideOwnerLookupTag)?.let { return it }
        return createAndCacheIrPropertySymbols(property, fakeOverrideOwnerLookupTag)
    }

    private fun createAndCacheIrPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): PropertySymbols {
        val irParent = findIrParent(property, fakeOverrideOwnerLookupTag)
        if (irParent?.isExternalParent() == true) {
            val symbols = createPropertySymbols(property, fakeOverrideOwnerLookupTag, parentIsExternal = true)
            @OptIn(FirBasedFakeOverrideGenerator::class) // only for lazy
            val firForLazyProperty = calculateFirForLazyDeclaration(
                property, fakeOverrideOwnerLookupTag, irParent,
                fakeOverrideGenerator::createFirPropertyFakeOverrideIfNeeded
            )

            callablesGenerator.createIrProperty(
                firForLazyProperty,
                irParent,
                symbols,
                predefinedOrigin = firForLazyProperty.computeExternalOrigin(),
                allowLazyDeclarationsCreation = true
            ).also {
                check(it is Fir2IrLazyProperty)
            }

            cacheIrPropertySymbols(property, symbols, fakeOverrideOwnerLookupTag)
            return symbols
        }

        val symbols = createPropertySymbols(property, fakeOverrideOwnerLookupTag, parentIsExternal = false)
        cacheIrPropertySymbols(property, symbols, fakeOverrideOwnerLookupTag)

        return symbols
    }

    fun getCachedIrPropertySymbol(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature? = { signatureComposer.composeSignature(property, fakeOverrideOwnerLookupTag) }
    ): IrPropertySymbol? {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)
        val symbol = getCachedIrCallableSymbol(
            property,
            fakeOverrideOwnerLookupTag,
            propertyCache::get,
            propertyCache::set,
            signatureCalculator
        ) { signature ->
            symbolTable.referencePropertyIfAny(signature)
        } ?: return null
        return symbolsMappingForLazyClasses.remapPropertySymbol(symbol)
    }

    private fun getCachedIrPropertySymbols(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature? = { signatureComposer.composeSignature(property, fakeOverrideOwnerLookupTag) }
    ): PropertySymbols? {
        val propertySymbol = getCachedIrPropertySymbol(property, fakeOverrideOwnerLookupTag, signatureCalculator) ?: return null
        return PropertySymbols(
            propertySymbol,
            findGetterOfProperty(propertySymbol)!!,
            findSetterOfProperty(propertySymbol),
            findBackingFieldOfProperty(propertySymbol)
        )
    }

    fun findGetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return getterForPropertyCache[propertySymbol]?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    fun findSetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]?.let(symbolsMappingForLazyClasses::remapFunctionSymbol)
    }

    fun findBackingFieldOfProperty(propertySymbol: IrPropertySymbol): IrFieldSymbol? {
        return backingFieldForPropertyCache[propertySymbol]
    }

    fun findPropertyForBackingField(fieldSymbol: IrFieldSymbol): IrPropertySymbol? {
        return propertyForBackingFieldCache[fieldSymbol]
    }

    internal fun cacheDelegatedProperty(property: FirProperty, irProperty: IrProperty) {
        val symbol = irProperty.symbol
        propertyCache[property] = symbol
        delegatedReverseCache[symbol] = property
    }

    // ------------------------------------ fields ------------------------------------

    fun getOrCreateIrField(
        firFieldSymbol: FirFieldSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrField {
        val fir = firFieldSymbol.fir
        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(fir, fakeOverrideOwnerLookupTag)
        if (staticFakeOverrideKey == null) {
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            fieldCache[fir]?.ownerIfBound()?.let { return it }
        } else {
            generateLazyFakeOverrides(fir.name, fakeOverrideOwnerLookupTag)
            // Lazy static fake override should always exist
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            return fieldStaticOverrideCache[staticFakeOverrideKey]!!.owner
        }
        // In case of type parameters from the parent as the field's return type, find the parent ahead to cache type parameters.
        val irParent = findIrParent(fir, fakeOverrideOwnerLookupTag)

        val unwrapped = fir.unwrapFakeOverrides()
        if (unwrapped !== fir) {
            return getOrCreateIrField(unwrapped.symbol)
        }
        return createAndCacheIrField(fir, irParent)
    }

    // TODO: there is a mess with methods for fields
    //   we have three (!) different functions to getOrCreate field in different circumstances
    fun getOrCreateIrField(field: FirField, irParent: IrDeclarationParent?): IrField {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        getCachedIrFieldSymbol(field, irParent)?.ownerIfBound()?.let { return it }
        return createAndCacheIrField(field, irParent)
    }

    private fun getCachedIrFieldSymbol(field: FirField, irParent: IrDeclarationParent?): IrFieldSymbol? {
        val containingClassLookupTag = (irParent as IrClass?)?.classId?.toLookupTag()
        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(field, containingClassLookupTag)
        return if (staticFakeOverrideKey == null) {
            fieldCache[field]
        } else {
            fieldStaticOverrideCache[staticFakeOverrideKey]
        }
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
                    // local property cannot be referenced before declaration, so it's safe to take an owner from the symbol
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    val delegatedProperty = localStorage.getDelegatedProperty(fir)?.owner
                    return delegatedProperty?.delegate?.symbol ?: getIrVariableSymbol(fir)
                }
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                propertyCache[fir]?.ownerIfBound()?.let { return it.backingField!!.symbol }
                val irParent = findIrParent(fir, fakeOverrideOwnerLookupTag = null)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                createAndCacheIrProperty(fir, irParent, predefinedOrigin = parentOrigin).backingField!!.symbol
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    fun getCachedIrDelegateOrBackingFieldSymbol(field: FirField): IrFieldSymbol? {
        return fieldCache[field]
    }

    fun recordDelegateFieldMappedToBackingField(field: FirField, irFieldSymbol: IrFieldSymbol) {
        fieldCache[field] = irFieldSymbol
    }

    fun getCachedIrFieldStaticFakeOverrideSymbolByDeclaration(field: FirField): IrFieldSymbol? {
        val ownerLookupTag = field.containingClassLookupTag() ?: return null
        return fieldStaticOverrideCache[FieldStaticOverrideKey(ownerLookupTag, field.name)]
    }

    internal fun createDelegateIrField(field: FirField, irClass: IrClass): IrField {
        return createAndCacheIrField(
            field,
            irParent = irClass,
            type = field.initializer?.resolvedType ?: field.returnTypeRef.coneType,
            origin = IrDeclarationOrigin.DELEGATE
        )
    }

    private fun createAndCacheIrField(
        field: FirField,
        irParent: IrDeclarationParent?,
        type: ConeKotlinType = field.returnTypeRef.coneType,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField {
        val containingClassLookupTag = (irParent as IrClass?)?.classId?.toLookupTag()
        val symbol = createFieldSymbol()

        val irField = callablesGenerator.createIrField(field, irParent, symbol, type, origin)

        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(field, containingClassLookupTag)
        if (staticFakeOverrideKey == null) {
            fieldCache[field] = irField.symbol
        } else {
            fieldStaticOverrideCache[staticFakeOverrideKey] = irField.symbol
        }
        return irField
    }

    private fun createFieldSymbol(): IrFieldSymbol {
        return IrFieldSymbolImpl()
    }

    // This function returns null if this field/ownerClassId combination does not describe static fake override
    private fun getFieldStaticFakeOverrideKey(field: FirField, ownerLookupTag: ConeClassLikeLookupTag?): FieldStaticOverrideKey? {
        if (ownerLookupTag == null || !field.isStatic ||
            !field.isSubstitutionOrIntersectionOverride && ownerLookupTag == field.containingClassLookupTag()
        ) return null
        return FieldStaticOverrideKey(ownerLookupTag, field.name)
    }

    // ------------------------------------ parameters ------------------------------------

    fun createAndCacheParameter(
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
    ): IrValueParameter {
        return callablesGenerator.createIrParameter(
            valueParameter,
            index,
            useStubForDefaultValueStub,
            typeOrigin,
            skipDefaultParameter,
            forcedDefaultValueConversion
        ).also {
            localStorage.putParameter(valueParameter, it.symbol)
        }
    }

    // ------------------------------------ local delegated properties ------------------------------------

    fun findGetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol {
        return getterForPropertyCache.getValue(propertySymbol)
    }

    fun findSetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]
    }

    fun findDelegateVariableOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrVariableSymbol {
        return delegateVariableForPropertyCache.getValue(propertySymbol)
    }

    fun createAndCacheIrLocalDelegatedProperty(
        property: FirProperty,
        irParent: IrDeclarationParent
    ): IrLocalDelegatedProperty {
        val symbols = createLocalDelegatedPropertySymbols(property)
        val irProperty = callablesGenerator.createIrLocalDelegatedProperty(property, irParent, symbols)
        val symbol = irProperty.symbol
        delegateVariableForPropertyCache[symbol] = irProperty.delegate.symbol
        getterForPropertyCache[symbol] = irProperty.getter.symbol
        irProperty.setter?.let { setterForPropertyCache[symbol] = it.symbol }
        localStorage.putDelegatedProperty(property, symbol)
        return irProperty
    }

    private fun createLocalDelegatedPropertySymbols(property: FirProperty): LocalDelegatedPropertySymbols {
        val propertySymbol = IrLocalDelegatedPropertySymbolImpl()
        val getterSymbol = createFunctionSymbol(signature = null)
        val setterSymbol = runIf(property.isVar) {
            createFunctionSymbol(signature = null)
        }
        return LocalDelegatedPropertySymbols(propertySymbol, getterSymbol, setterSymbol)
    }

    // ------------------------------------ variables ------------------------------------

    fun createAndCacheIrVariable(
        variable: FirVariable,
        irParent: IrDeclarationParent,
        givenOrigin: IrDeclarationOrigin? = null
    ): IrVariable {
        return callablesGenerator.createIrVariable(variable, irParent, givenOrigin).also {
            localStorage.putVariable(variable, it.symbol)
        }
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                classifierStorage.getCachedIrEnumEntry(firDeclaration)?.let { return it.symbol }
                val irParentClass = firDeclaration.containingClassLookupTag()?.let { classifierStorage.findIrClass(it) }!!

                val containingFile = firProvider.getFirCallableContainerFile(firVariableSymbol)

                classifierStorage.getOrCreateIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    predefinedOrigin = if (containingFile != null) IrDeclarationOrigin.DEFINED else irParentClass.origin
                ).symbol
            }
            is FirValueParameter -> {
                localStorage.getParameter(firDeclaration)
                // catch parameter is FirValueParameter in FIR but IrVariable in IR
                    ?: return getIrVariableSymbol(firDeclaration)
            }
            else -> {
                getIrVariableSymbol(firDeclaration)
            }
        }
    }

    private fun getIrVariableSymbol(firVariable: FirVariable): IrVariableSymbol {
        return localStorage.getVariable(firVariable)
            ?: error("Cannot find variable ${firVariable.render()} in local storage")
    }

    // ------------------------------------ anonymous initializers ------------------------------------

    fun createIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        containingIrClass: IrClass,
    ): IrAnonymousInitializer {
        val irInitializer = callablesGenerator.createIrAnonymousInitializer(anonymousInitializer, containingIrClass)
        val alreadyContained = initializerCache.put(anonymousInitializer, irInitializer)
        require(alreadyContained == null) {
            "IR for anonymous initializer already exits: ${anonymousInitializer.render()}"
        }
        return irInitializer
    }

    fun getIrAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): IrAnonymousInitializer {
        return initializerCache.getValue(anonymousInitializer)
    }

    // ------------------------------------ callables ------------------------------------

    fun originalDeclarationForDelegated(irDeclaration: IrDeclaration): FirDeclaration? {
        return delegatedReverseCache[irDeclaration.symbol]
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

    private fun FirCallableDeclaration.computeExternalOrigin(): IrDeclarationOrigin {
        val containingClass = containingClassLookupTag()?.toFirRegularClass(session)
        return when (containingClass?.isJavaOrEnhancement) {
            true -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
        }
    }

    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        isLocal: Boolean = false
    ): IrFunctionSymbol {
        val function = firFunctionSymbol.fir

        if (function is FirConstructor) {
            return getIrConstructorSymbol(function.symbol)
        }

        getCachedIrFunctionSymbol(function, fakeOverrideOwnerLookupTag)?.let { return it }
        if (function is FirSimpleFunction && !isLocal) {
            val irParent = findIrParent(function, fakeOverrideOwnerLookupTag)
            if (irParent?.isExternalParent() == true) {
                val symbol = createMemberFunctionSymbol(function, fakeOverrideOwnerLookupTag, parentIsExternal = true)
                @OptIn(FirBasedFakeOverrideGenerator::class) // only for lazy
                val firForLazyFunction = calculateFirForLazyDeclaration(
                    function, fakeOverrideOwnerLookupTag, irParent,
                    fakeOverrideGenerator::createFirFunctionFakeOverrideIfNeeded
                )
                // Return value is not used here, because creation of IR declaration binds it to the corresponding symbol
                // And all we want here is to bind symbol for lazy declaration
                callablesGenerator.createIrFunction(
                    firForLazyFunction,
                    irParent,
                    symbol,
                    predefinedOrigin = firForLazyFunction.computeExternalOrigin(),
                    isLocal = false,
                    fakeOverrideOwnerLookupTag,
                    allowLazyDeclarationsCreation = true
                ).also {
                    check(it is Fir2IrLazySimpleFunction)
                }
                cacheIrFunctionSymbol(function, symbol, fakeOverrideOwnerLookupTag)
                return symbol
            }
        }

        val symbol = createMemberFunctionSymbol(function, fakeOverrideOwnerLookupTag, parentIsExternal = false)
        cacheIrFunctionSymbol(function, symbol, fakeOverrideOwnerLookupTag)
        return symbol
    }

    private inline fun <reified FC : FirCallableDeclaration, reified IS : IrSymbol> getCachedIrCallableSymbol(
        declaration: FC,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        cacheGetter: (FC) -> IS?,
        cacheSetter: (FC, IS) -> Unit,
        signatureCalculator: () -> IdSignature?,
        referenceIfAny: (IdSignature) -> IS?
    ): IS? {
        /*
         * There should be two types of declarations:
         * 1. Real declarations. They are stored in simple FirDeclaration -> IrDeclaration [cache]
         * 2. Fake overrides. They are stored in [irFakeOverridesForRealFirFakeOverrideMap], where the key is the original real declaration and
         *      specific dispatch receiver of particular fake override. This cache is needed, because we can have two different FIR
         *      f/o for common and platform modules (because they are session dependent), but we should create IR declaration for them
         *      only once. So [irFakeOverridesForFirFakeOverrideMap] is shared between fir2ir conversion for different MPP modules
         *      (see KT-58229)
         *
         * Unfortunately, in the current implementation, there is a special case.
         * If the fake override exists in FIR (i.e., it is an intersection or substitution override), and it comes from dependency module,
         * corresponding LazyIrFunction or LazyIrProperty can be created, ignoring the fact that it is a fake override.
         * In that case, it can sometimes be put to the wrong cache, as a normal declaration.
         *
         * To workaround this, we look up such declarations in both caches.
         */
        val isFakeOverride = declaration.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag)
        if (isFakeOverride) {
            val key = FakeOverrideIdentifier(
                declaration.unwrapFakeOverridesOrDelegated().symbol,
                fakeOverrideOwnerLookupTag ?: declaration.containingClassLookupTag()!!
            )
            irForFirSessionDependantDeclarationMap[key]?.let { return it as IS }
        } else {
            cacheGetter(declaration)?.let { return it }
        }

        // TODO: Special case mentioned above. Should be removed after fixing creation. KT-61085
        if (declaration.isSubstitutionOrIntersectionOverride) {
            cacheGetter(declaration)?.let { return it }
        }

        /*
         * There are cases when two different f/o identifiers may represent the same IR f/o
         *
         * // MODULE: common
         * expect open class Base<T>() {
         *     fun foo(param: T) // (1)
         * }
         *
         * class Derived : Base<String>() {
         *     // substitution override fun foo(param: String)
         * }
         *
         * // MODULE: platform()()(common)
         * actual open class Base<T> {
         *     actual fun foo(param: T) {} // (2)
         * }
         *
         * fun test(d: Derived) {
         *     d.foo()
         * }
         *
         * In this case we have two different FIR functions for substitution override Derived.foo, because substitution and two different
         *   original functions for them depending on the module we are watching
         * - during conversion of the common module we will create and save IR f/o for derived with identifier (function (1), Derived)
         * - during conversion of the platform module for each call of `Derived.foo` we will use identifier (function (2), Derived).
         * But we actually must use the f/o which was created in common module. So here we should reuse the symbol from symbol table if we
         *   find one.
         *
         * TODO: Most likely check for `isFakeOverride` may be removed after fix of KT-61774
         */
        signatureCalculator()?.let { signature ->
            val cachedInSymbolTable = referenceIfAny(signature) ?: return@let
            when {
                isFakeOverride -> {
                    val key = FakeOverrideIdentifier(
                        declaration.symbol.unwrapFakeOverrides(),
                        fakeOverrideOwnerLookupTag ?: declaration.containingClassLookupTag()!!
                    )
                    irForFirSessionDependantDeclarationMap[key] = cachedInSymbolTable
                }
                configuration.useIrFakeOverrideBuilder -> {
                    /*
                     * If IR fake override builder is used for building fake-overrides, they are generated bypassing Fir2IrDeclarationStorage,
                     *   and are written directly to SymbolTable. So in this case it is normal to save the result from symbol table into
                     *   storage
                     *
                     * TODO: potentially this situation won't happen after migration from FIR2IR f/o generator to IR f/o generator (see KT-58861)
                     */
                    cacheSetter(declaration, cachedInSymbolTable)
                }
                declaration.initialSignatureAttr != null -> {
                    /*
                     * FIR creates remapped functions for builtin JVM classes based on use-site session, not declaration site
                     * It leads to the situations when we have two different mapped FIR functions for the same original function
                     *   (and same IR function)
                     */
                    cacheSetter(declaration, cachedInSymbolTable)
                }
                declaration.symbol is FirJavaOverriddenSyntheticPropertySymbol -> {
                    /*
                     * Synthetic properties for java classes, if those properties are based on real Kotlin properties are also session
                     *   dependant
                     */
                    cacheSetter(declaration, cachedInSymbolTable)
                }
                declaration.origin.generatedAnyMethod -> {
                    /*
                     * Generated methods from Any for data and value classes are session-dependant
                     */
                    cacheSetter(declaration, cachedInSymbolTable)
                }
                else -> {
                    error("IR declaration with signature \"$signature\" found in SymbolTable and not found in declaration storage")
                }
            }
            return cachedInSymbolTable
        }

        return null
    }

    private inline fun <T : FirCallableDeclaration> calculateFirForLazyDeclaration(
        originalDeclaration: T,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        irParent: IrDeclarationParent,
        createFakeOverrideIfNeeded: (T, ConeClassLikeLookupTag, IrClass) -> T?
    ): T {
        if (irParent !is IrClass || fakeOverrideOwnerLookupTag == null) return originalDeclaration
        return createFakeOverrideIfNeeded(originalDeclaration, fakeOverrideOwnerLookupTag, irParent) ?: originalDeclaration
    }

    private fun generateLazyFakeOverrides(name: Name, fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?) {
        val firClassSymbol = fakeOverrideOwnerLookupTag?.toSymbol(session) as? FirClassSymbol
        if (firClassSymbol != null) {
            val irClass = classifierStorage.getOrCreateIrClass(firClassSymbol)
            if (irClass is Fir2IrLazyClass) {
                irClass.getFakeOverridesByName(name)
            }
        }
    }

    // ------------------------------------ binding unbound symbols ------------------------------------

    /**
     * This function iterates over all f/o symbols created in declaration storage and binds all unbound symbols
     *
     * Usually all symbols are bound after fir2ir conversion is over, but there is a case in MPP scenario when some fake-override
     *   for common classes appears only during conversion of platform session:
     *
     * // MODULE: common
     * expect interface A
     *
     * interface B : A {
     *     // f/o fun foo() // (1)
     * }
     *
     * // MODULE: platform()()(common)
     * actual interface A {
     *     fun foo() // (2)
     * }
     *
     * fun test(b: B) {
     *     b.foo() // (3)
     * }
     *
     * Here during common module conversion there is no `foo` function in scope of class B, so (1) is not generated
     * During conversion of function test we reference symbol for (1) at line (3), so this symbol is created. But
     *   there is no code which generate actual IR for this symbol, because IR for f/o is generated only during
     *   conversion of corresponing class (and `B` is already converted)
     *
     * So to fix this issue we need to call this method after conversion of platform module
     */
    @LeakedDeclarationCaches
    internal fun generateUnboundFakeOverrides() {
        for ((identifier, symbol) in irForFirSessionDependantDeclarationMap) {
            if (symbol.isBound) continue
            val (originalSymbol, dispatchReceiverLookupTag, _) = identifier
            generateDeclaration(originalSymbol, dispatchReceiverLookupTag)
        }
    }

    /**
     * This function iterates over all non f/o callable symbols created in declaration storage and binds all unbound symbols
     *
     * Usually all symbols are bound after fir2ir conversion is over, but it's not true for `allowNonCachedDeclarations`, when
     *   we convert to IR only part of sources from code fragments.
     *
     * ```
     * // Original code
     * fun foo(x: Int) {} // (1)
     *
     * fun bar() {
     *     1.let { // (2)
     *         <context of code fragment>
     *     }
     * }
     *
     * // Code fragment
     * foo(this@let)
     *
     * Here in the body of the code fragment we reference function (1) and lambda (2), which leads to creation of their symbols,
     *   but not to generation of their IR. And since the original code won't be processed by fir2ir, we need to manually create
     *   IR for all symbols from it, to avoid publication of unbound symbols after fir2ri conversion is over
     *
     * Note that in the code fragment we may capture even local functions and lambdas, which are stored not in global caches,
     *   but in `localStorage`, which is getting cleared after leaving from corresponding scope. So to generate IR for them we need
     *   to call this function not only after fir2ir conversion, but also after leaving each local scope (see `leaveScope` function)
     */
    @LeakedDeclarationCaches
    internal fun fillUnboundSymbols() {
        fillUnboundSymbols(functionCache)
        fillUnboundSymbols(propertyCache.normal)
        fillUnboundSymbols(propertyCache.synthetic)
    }

    @LeakedDeclarationCaches
    private fun fillUnboundSymbols(cache: Map<out FirCallableDeclaration, IrSymbol>) {
        for ((firDeclaration, irSymbol) in cache) {
            if (irSymbol.isBound) continue
            generateDeclaration(firDeclaration.symbol, dispatchReceiverLookupTag = null)
        }
    }

    private fun generateDeclaration(
        originalSymbol: FirBasedSymbol<*>,
        dispatchReceiverLookupTag: ConeClassLikeLookupTag?,
    ) {
        val irParent = findIrParent(
            originalSymbol.packageFqName(),
            dispatchReceiverLookupTag ?: originalSymbol.getContainingClassSymbol(session)?.toLookupTag(),
            originalSymbol,
            originalSymbol.origin
        )
        when (originalSymbol) {
            is FirPropertySymbol -> createAndCacheIrProperty(
                originalSymbol.fir,
                irParent,
                fakeOverrideOwnerLookupTag = dispatchReceiverLookupTag
            )

            is FirNamedFunctionSymbol -> createAndCacheIrFunction(
                originalSymbol.fir,
                irParent,
                fakeOverrideOwnerLookupTag = dispatchReceiverLookupTag
            )

            else -> error("Unexpected declaration: $originalSymbol")
        }
    }

    // ------------------------------------ scripts ------------------------------------

    fun getCachedIrScript(script: FirScript): IrScript? {
        return scriptCache[script]
    }

    fun createIrScript(script: FirScript): IrScript {
        getCachedIrScript(script)?.let { error("IrScript already created: ${script.render()}") }
        val symbol = IrScriptSymbolImpl()
        return callablesGenerator.createIrScript(script, symbol).also {
            scriptCache[script] = it
        }
    }

    // ------------------------------------ scoping ------------------------------------

    fun enterScope(symbol: IrSymbol) {
        symbolTable.enterScope(symbol)
        if (symbol is IrSimpleFunctionSymbol ||
            symbol is IrConstructorSymbol ||
            symbol is IrAnonymousInitializerSymbol ||
            symbol is IrPropertySymbol ||
            symbol is IrEnumEntrySymbol ||
            symbol is IrScriptSymbol
        ) {
            localStorage.enterCallable()
        }
    }

    fun leaveScope(symbol: IrSymbol) {
        if (symbol is IrSimpleFunctionSymbol ||
            symbol is IrConstructorSymbol ||
            symbol is IrAnonymousInitializerSymbol ||
            symbol is IrPropertySymbol ||
            symbol is IrEnumEntrySymbol ||
            symbol is IrScriptSymbol
        ) {
            if (configuration.allowNonCachedDeclarations) {
                // See KDoc to `fillUnboundSymbols` function
                @OptIn(LeakedDeclarationCaches::class)
                fillUnboundSymbols(localStorage.lastCache.localFunctions)
            }
            localStorage.leaveCallable()
        }
        symbolTable.leaveScope(symbol)
    }

    inline fun withScope(symbol: IrSymbol, crossinline block: () -> Unit) {
        enterScope(symbol)
        block()
        leaveScope(symbol)
    }

    // ------------------------------------ utilities ------------------------------------

    internal fun findIrParent(
        packageFqName: FqName,
        parentLookupTag: ConeClassLikeLookupTag?,
        firBasedSymbol: FirBasedSymbol<*>,
        firOrigin: FirDeclarationOrigin
    ): IrDeclarationParent? {
        if (parentLookupTag != null) {
            return classifierStorage.findIrClass(parentLookupTag)
        }


        // TODO: All classes from BUILT_INS_PACKAGE_FQ_NAMES are considered built-ins now,
        // which is not exact and can lead to some problems
        val parentPackage = getIrExternalOrBuiltInsPackageFragment(
            packageFqName, firBasedSymbol.moduleData, firOrigin,
            allowBuiltins = firBasedSymbol !is FirCallableSymbol<*>
        )

        /**
         * In `allowNonCachedDeclarations` mode there is a situation possible when we get source declaration
         *   from session which is different from one which we convert right now. So we need to take an original firProvider
         *   for this declaration to correctly find containig file and properly generate NonCachedSourceFileFacadeClass if needed
         */
        val firProvider = if (configuration.allowNonCachedDeclarations) {
            when {
                firBasedSymbol.moduleData == session.moduleData -> components.firProvider
                else -> firBasedSymbol.moduleData.session.firProvider
            }
        } else {
            components.firProvider
        }

        val containerFile = when (firBasedSymbol) {
            is FirCallableSymbol -> firProvider.getFirCallableContainerFile(firBasedSymbol)
            is FirClassLikeSymbol -> firProvider.getFirClassifierContainerFileIfAny(firBasedSymbol)
            else -> error("Unknown symbol: $firBasedSymbol")
        }

        if (containerFile != null) {
            val existingFile = fileCache[containerFile]
            if (existingFile != null) {
                return existingFile
            }

            // Sudden declarations do not go through IR lowering process,
            // so the parent file isn't replaced with a facade class, as in 'FileClassLowering'.
            if (configuration.allowNonCachedDeclarations && firBasedSymbol is FirCallableSymbol<*>) {
                val psiFile = containerFile.psi?.containingFile
                if (psiFile is KtFile) {
                    val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(psiFile)
                    val className = JvmClassName.byFqNameWithoutInnerClasses(fileClassInfo.fileClassFqName)

                    val facadeClassName: JvmClassName?
                    val declarationOrigin: IrDeclarationOrigin

                    if (fileClassInfo.withJvmMultifileClass) {
                        facadeClassName = JvmClassName.byFqNameWithoutInnerClasses(fileClassInfo.facadeClassFqName)
                        declarationOrigin = IrDeclarationOrigin.JVM_MULTIFILE_CLASS
                    } else {
                        facadeClassName = null
                        declarationOrigin = IrDeclarationOrigin.FILE_CLASS
                    }

                    val facadeShortName = className.fqNameForClassNameWithoutDollars.shortName()
                    val containerSource = NonCachedSourceFacadeContainerSource(className, facadeClassName)
                    return NonCachedSourceFileFacadeClass(declarationOrigin, facadeShortName, containerSource).apply {
                        parent = parentPackage
                        createParameterDeclarations()
                    }
                }
            }
        }

        return parentPackage
    }

    private fun findIrParent(
        callableDeclaration: FirCallableDeclaration,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ): IrDeclarationParent? {
        val firBasedSymbol = callableDeclaration.symbol
        val callableId = firBasedSymbol.callableId
        val callableOrigin = callableDeclaration.origin
        val parentLookupTag = when {
            // non-static fields can not be fake overrides
            firBasedSymbol is FirFieldSymbol && !firBasedSymbol.isStatic -> callableDeclaration.containingClassLookupTag()
            else -> fakeOverrideOwnerLookupTag ?: callableDeclaration.containingClassLookupTag()
        }
        return findIrParent(
            callableId.packageName,
            parentLookupTag,
            firBasedSymbol,
            callableOrigin
        )
    }

    companion object {
        internal val ENUM_SYNTHETIC_NAMES = mapOf(
            Name.identifier("values") to IrSyntheticBodyKind.ENUM_VALUES,
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF,
            Name.identifier("entries") to IrSyntheticBodyKind.ENUM_ENTRIES,
            Name.special("<get-entries>") to IrSyntheticBodyKind.ENUM_ENTRIES
        )
    }
}

private fun FirCallableDeclaration.isFakeOverrideOrDelegated(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (isCopyCreatedInScope) return true
    return isFakeOverrideImpl(fakeOverrideOwnerLookupTag)
}

private fun FirCallableDeclaration.isFakeOverride(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (isSubstitutionOrIntersectionOverride) return true
    return isFakeOverrideImpl(fakeOverrideOwnerLookupTag)
}

private fun FirCallableDeclaration.isFakeOverrideImpl(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (fakeOverrideOwnerLookupTag == null) return false
    // this condition is true for all places when we are trying to create "fake" fake overrides in IR
    // "fake" fake overrides are f/o which are presented in IR but have no corresponding FIR f/o
    return fakeOverrideOwnerLookupTag != containingClassLookupTag()
}

private object IsStubPropertyForPureFieldKey : FirDeclarationDataKey()

internal var FirProperty.isStubPropertyForPureField: Boolean? by FirDeclarationDataRegistry.data(IsStubPropertyForPureFieldKey)

/**
 * Opt-in to this annotation indicates that some code uses annotated function but it actually shouldn't
 * See KT-61513
 */
@RequiresOptIn
annotation class LeakedDeclarationCaches

/**
 * This function is introduced as preparation to publishing unbound symbols in fir2ir
 * There is a probability that it won't be non needed in future, but for now it allows
 *   to easily track all places left when we need to extract owner from symbol
 */
@UnsafeDuringIrConstructionAPI
internal fun <D : IrDeclaration> IrBindableSymbol<*, D>.ownerIfBound(): D? {
    return runIf(isBound) { owner }
}

data class PropertySymbols(
    val propertySymbol: IrPropertySymbol,
    val getterSymbol: IrSimpleFunctionSymbol,
    val setterSymbol: IrSimpleFunctionSymbol?,
    val backingFieldSymbol: IrFieldSymbol?,
)

data class LocalDelegatedPropertySymbols(
    val propertySymbol: IrLocalDelegatedPropertySymbol,
    val getterSymbol: IrSimpleFunctionSymbol,
    val setterSymbol: IrSimpleFunctionSymbol?,
)
