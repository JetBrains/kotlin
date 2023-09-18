/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.references.toResolvedValueParameterSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation

@OptIn(LeakedDeclarationCaches::class)
class Fir2IrDeclarationStorage(
    private val components: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor,
    commonMemberStorage: Fir2IrCommonMemberStorage
) : Fir2IrComponents by components {

    private val fragmentCache: ConcurrentHashMap<FqName, ExternalPackageFragments> = ConcurrentHashMap()

    private class ExternalPackageFragments(
        val fragmentForDependencies: IrExternalPackageFragment,
        val fragmentForPrecompiledBinaries: IrExternalPackageFragment
    )

    private val builtInsFragmentCache: ConcurrentHashMap<FqName, IrExternalPackageFragment> = ConcurrentHashMap()

    private val fileCache: ConcurrentHashMap<FirFile, IrFile> = ConcurrentHashMap()

    private val scriptCache: ConcurrentHashMap<FirScript, IrScript> = ConcurrentHashMap()

    private val functionCache: ConcurrentHashMap<FirFunction, IrSimpleFunction> = commonMemberStorage.functionCache

    private val constructorCache: ConcurrentHashMap<FirConstructor, IrConstructor> = commonMemberStorage.constructorCache

    private val initializerCache: ConcurrentHashMap<FirAnonymousInitializer, IrAnonymousInitializer> = ConcurrentHashMap()

    private val propertyCache: ConcurrentHashMap<FirProperty, IrProperty> = commonMemberStorage.propertyCache
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

    /*
     * FIR declarations for substitution and intersection overrides are session dependent, which means that in MPP project
     *   we can have two different functions for the same substitution overrides (in common and platform modules)
     *
     * So this cache is needed to have only one IR declaration for both overrides
     *
     * The key here is a pair of the original function (first not f/o) and lookup tag of class for which this fake override was created
     * THe value is IR function, build for this fake override during fir2ir translation of the module that contains parent class of this function
     */
    private val irFakeOverridesForFirFakeOverrideMap: MutableMap<FakeOverrideIdentifier, IrDeclaration> =
        commonMemberStorage.irFakeOverridesForFirFakeOverrideMap

    data class FakeOverrideIdentifier(val originalSymbol: FirCallableSymbol<*>, val dispatchReceiverLookupTag: ConeClassLikeLookupTag)

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

    // ------------------------------------ package fragments ------------------------------------

    fun getIrExternalPackageFragment(
        fqName: FqName,
        firOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): IrExternalPackageFragment {
        val fragments = fragmentCache.getOrPut(fqName) {
            val fragmentForDependencies = callablesGenerator.createExternalPackageFragment(
                fqName, FirModuleDescriptor(session, moduleDescriptor.builtIns)
            )
            val fragmentForPrecompiledBinaries = callablesGenerator.createExternalPackageFragment(fqName, moduleDescriptor)
            ExternalPackageFragments(fragmentForDependencies, fragmentForPrecompiledBinaries)
        }
        // Make sure that external package fragments have a different module descriptor. The module descriptors are compared
        // to determine if objects need regeneration because they are from different modules.
        // But keep original module descriptor for the fragments coming from parts compiled on the previous incremental step
        return when (firOrigin) {
            FirDeclarationOrigin.Precompiled -> fragments.fragmentForPrecompiledBinaries
            else -> fragments.fragmentForDependencies
        }
    }

    private fun getIrExternalOrBuiltInsPackageFragment(fqName: FqName, firOrigin: FirDeclarationOrigin): IrExternalPackageFragment {
        val isBuiltIn = fqName in BUILT_INS_PACKAGE_FQ_NAMES
        return if (isBuiltIn) getIrBuiltInsPackageFragment(fqName) else getIrExternalPackageFragment(fqName, firOrigin)
    }

    private fun getIrBuiltInsPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return builtInsFragmentCache.getOrPut(fqName) {
            callablesGenerator.createExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
        }
    }

    // ------------------------------------ files ------------------------------------

    fun registerFile(firFile: FirFile, irFile: IrFile) {
        fileCache[firFile] = irFile
    }

    fun getIrFile(firFile: FirFile): IrFile {
        return fileCache[firFile]!!
    }

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

    fun getCachedIrFunction(function: FirFunction): IrSimpleFunction? {
        return if (function is FirSimpleFunction) getCachedIrFunction(function)
        else localStorage.getLocalFunction(function)
    }

    fun getCachedIrFunction(function: FirSimpleFunction): IrSimpleFunction? {
        return getCachedIrFunction(function, fakeOverrideOwnerLookupTag = null) { signatureComposer.composeSignature(function) }
    }

    fun getCachedIrFunction(
        function: FirSimpleFunction,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrSimpleFunction? {
        if (function.visibility == Visibilities.Local) {
            return localStorage.getLocalFunction(function)
        }
        val cachedIrCallable = getCachedIrCallable(
            function,
            fakeOverrideOwnerLookupTag,
            functionCache,
            signatureCalculator
        ) { signature ->
            @OptIn(IrSymbolInternals::class)
            symbolTable.referenceSimpleFunctionIfAny(signature)?.owner
        }
        return cachedIrCallable
    }

    fun getOrCreateIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrSimpleFunction {
        getCachedIrFunction(function)?.let { return it }
        return createAndCacheIrFunction(function, irParent, predefinedOrigin, isLocal, fakeOverrideOwnerLookupTag)
    }

    @LeakedDeclarationCaches
    fun createAndCacheIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrSimpleFunction {
        val irFunction = callablesGenerator.createIrFunction(
            function,
            irParent,
            predefinedOrigin,
            isLocal = isLocal,
            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag
        )
        cacheIrFunction(function, irFunction, fakeOverrideOwnerLookupTag)

        return irFunction
    }

    private fun cacheIrFunction(function: FirFunction, irFunction: IrSimpleFunction, fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?) {
        when {
            irFunction.visibility == DescriptorVisibilities.LOCAL -> {
                localStorage.putLocalFunction(function, irFunction)
            }

            function.isFakeOverride(fakeOverrideOwnerLookupTag) -> {
                val originalFunction = function.unwrapFakeOverrides()
                val key = FakeOverrideIdentifier(
                    originalFunction.symbol,
                    fakeOverrideOwnerLookupTag ?: function.containingClassLookupTag()!!
                )
                irFakeOverridesForFirFakeOverrideMap[key] = irFunction
            }

            else -> {
                functionCache[function] = irFunction
            }
        }
    }

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters.drop(contextReceivers.size))) {
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    internal fun cacheDelegationFunction(function: FirSimpleFunction, irFunction: IrSimpleFunction) {
        functionCache[function] = irFunction
        delegatedReverseCache[irFunction] = function
    }

    internal fun cacheGeneratedFunction(firFunction: FirSimpleFunction, irFunction: IrSimpleFunction) {
        functionCache[firFunction] = irFunction
    }

    // ------------------------------------ constructors ------------------------------------

    fun getCachedIrConstructor(
        constructor: FirConstructor,
        signatureCalculator: () -> IdSignature? = { null }
    ): IrConstructor? {
        return constructorCache[constructor] ?: signatureCalculator()?.let { signature ->
            symbolTable.referenceConstructorIfAny(signature)?.let { irConstructorSymbol ->
                @OptIn(IrSymbolInternals::class)
                val irConstructor = irConstructorSymbol.owner
                constructorCache[constructor] = irConstructor
                irConstructor
            }
        }
    }

    fun getOrCreateIrConstructor(
        constructor: FirConstructor,
        irParent: IrClass,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
    ): IrConstructor {
        getCachedIrConstructor(constructor)?.let { return it }
        return callablesGenerator.createIrConstructor(constructor, irParent, predefinedOrigin, isLocal)
    }

    @LeakedDeclarationCaches
    internal fun cacheIrConstructor(constructor: FirConstructor, irConstructor: IrConstructor) {
        constructorCache[constructor] = irConstructor
    }

    fun getIrConstructorSymbol(firConstructorSymbol: FirConstructorSymbol): IrConstructorSymbol {
        val fir = firConstructorSymbol.fir
        return getIrCallableSymbol(
            firConstructorSymbol,
            fakeOverrideOwnerLookupTag = null,
            getCachedIrDeclaration = { constructor: FirConstructor, _, calculator -> getCachedIrConstructor(constructor, calculator) },
            createIrDeclaration = { parent, origin ->
                callablesGenerator.createIrConstructor(fir, parent as IrClass, predefinedOrigin = origin)
            },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                val irConstructor = lazyDeclarationsGenerator.createIrLazyConstructor(fir, signature, declarationOrigin, lazyParent)
                constructorCache[fir] = irConstructor
                // NB: this is needed to prevent recursions in case of self bounds
                (irConstructor as Fir2IrLazyConstructor).prepareTypeParameters()
                irConstructor
            },
        ) as IrConstructorSymbol
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

    private fun preparePropertySymbol(symbol: FirPropertySymbol): FirPropertySymbol {
        return prepareProperty(symbol.fir).symbol
    }

    fun getOrCreateIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrProperty {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)
        getCachedIrProperty(property)?.let { return it }
        return createAndCacheIrProperty(property, irParent, predefinedOrigin, isLocal, fakeOverrideOwnerLookupTag)
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
                fakeOverrideOwnerLookupTag = containingClassId?.toLookupTag()
            )
        }
    }

    @LeakedDeclarationCaches
    fun createAndCacheIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrProperty {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)
        val irProperty = callablesGenerator.createIrProperty(property, irParent, predefinedOrigin, isLocal, fakeOverrideOwnerLookupTag)
        cacheIrProperty(property, irProperty, fakeOverrideOwnerLookupTag)
        return irProperty
    }

    private fun cacheIrProperty(
        property: FirProperty,
        irProperty: IrProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
    ) {
        val irPropertySymbol = irProperty.symbol
        irProperty.backingField?.symbol?.let {
            backingFieldForPropertyCache[irPropertySymbol] = it
            propertyForBackingFieldCache[it] = irPropertySymbol
        }
        irProperty.getter?.let {
            getterForPropertyCache[irPropertySymbol] = it.symbol
        }
        irProperty.setter?.let {
            setterForPropertyCache[irPropertySymbol] = it.symbol
        }
        if (property.isFakeOverride(fakeOverrideOwnerLookupTag)) {
            val originalProperty = property.unwrapFakeOverrides()
            val key = FakeOverrideIdentifier(
                originalProperty.symbol,
                fakeOverrideOwnerLookupTag ?: property.containingClassLookupTag()!!
            )
            irFakeOverridesForFirFakeOverrideMap[key] = irProperty
        } else {
            propertyCache[property] = irProperty
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

    fun getIrPropertySymbol(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrSymbol {
        @Suppress("NAME_SHADOWING")
        val firPropertySymbol = preparePropertySymbol(firPropertySymbol)
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
                createAndCacheIrProperty(
                    fir, parent, predefinedOrigin = origin, fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                )
            },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                lazyDeclarationsGenerator.createIrLazyProperty(fir, signature, lazyParent, declarationOrigin).also {
                    cacheIrProperty(fir, it, fakeOverrideOwnerLookupTag = null)
                }
            },
        )

        val originalSymbol = fakeOverrideOwnerLookupTag.getIrCallableSymbol()

        @OptIn(IrSymbolInternals::class)
        val originalProperty = originalSymbol.owner as IrProperty

        fun IrProperty.isIllegalFakeOverride(): Boolean {
            if (!isFakeOverride) return false
            val overriddenSymbols = overriddenSymbols
            @OptIn(IrSymbolInternals::class)
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

    fun getCachedIrProperty(property: FirProperty): IrProperty? {
        return getCachedIrProperty(property, fakeOverrideOwnerLookupTag = null) {
            signatureComposer.composeSignature(property)
        }
    }

    fun getCachedIrProperty(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrProperty? {
        @Suppress("NAME_SHADOWING")
        val property = prepareProperty(property)
        return getCachedIrCallable(
            property,
            fakeOverrideOwnerLookupTag,
            propertyCache,
            signatureCalculator
        ) { signature ->
            @OptIn(IrSymbolInternals::class)
            symbolTable.referencePropertyIfAny(signature)?.owner
        }
    }

    fun findGetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return getterForPropertyCache[propertySymbol]
    }

    fun findSetterOfProperty(propertySymbol: IrPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]
    }

    fun findBackingFieldOfProperty(propertySymbol: IrPropertySymbol): IrFieldSymbol? {
        return backingFieldForPropertyCache[propertySymbol]
    }

    fun findPropertyForBackingField(fieldSymbol: IrFieldSymbol): IrPropertySymbol? {
        return propertyForBackingFieldCache[fieldSymbol]
    }

    internal fun cacheDelegatedProperty(property: FirProperty, irProperty: IrProperty) {
        propertyCache[property] = irProperty
        delegatedReverseCache[irProperty] = property
    }

    // ------------------------------------ fields ------------------------------------

    fun getOrCreateIrField(
        firFieldSymbol: FirFieldSymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
    ): IrField {
        val fir = firFieldSymbol.fir
        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(fir, fakeOverrideOwnerLookupTag)
        if (staticFakeOverrideKey == null) {
            fieldCache[fir]?.let { return it }
        } else {
            generateLazyFakeOverrides(fir.name, fakeOverrideOwnerLookupTag)
            // Lazy static fake override should always exist
            return fieldStaticOverrideCache[staticFakeOverrideKey]!!
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
        getCachedIrField(field, irParent)?.let { return it }
        return createAndCacheIrField(field, irParent)
    }

    private fun getCachedIrField(field: FirField, irParent: IrDeclarationParent?): IrField? {
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
                    return localStorage.getDelegatedProperty(fir)?.delegate?.symbol ?: getIrVariableSymbol(fir)
                }
                propertyCache[fir]?.let { return it.backingField!!.symbol }
                val irParent = findIrParent(fir, fakeOverrideOwnerLookupTag = null)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                createAndCacheIrProperty(fir, irParent, predefinedOrigin = parentOrigin).backingField!!.symbol
            }
            else -> {
                getIrVariableSymbol(fir)
            }
        }
    }

    fun getCachedIrDelegateOrBackingField(field: FirField): IrField? {
        return fieldCache[field]
    }

    fun getCachedIrFieldStaticFakeOverrideByDeclaration(field: FirField): IrField? {
        val ownerLookupTag = field.containingClassLookupTag() ?: return null
        return fieldStaticOverrideCache[FieldStaticOverrideKey(ownerLookupTag, field.name)]
    }

    internal fun getOrCreateDelegateIrField(field: FirField, owner: FirClass, irClass: IrClass): IrField {
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
        return createAndCacheIrField(
            field,
            irParent = irClass,
            type = initializer?.resolvedType ?: field.returnTypeRef.coneType,
            origin = IrDeclarationOrigin.DELEGATE
        )
    }

    private fun createAndCacheIrField(
        field: FirField,
        irParent: IrDeclarationParent?,
        type: ConeKotlinType = field.returnTypeRef.coneType,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField {
        val irField = callablesGenerator.createIrField(field, irParent, type, origin)
        val containingClassLookupTag = (irParent as IrClass?)?.classId?.toLookupTag()
        val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(field, containingClassLookupTag)
        if (staticFakeOverrideKey == null) {
            fieldCache[field] = irField
        } else {
            fieldStaticOverrideCache[staticFakeOverrideKey] = irField
        }
        return irField
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
            localStorage.putParameter(valueParameter, it)
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
        val irProperty = callablesGenerator.createIrLocalDelegatedProperty(property, irParent)
        val symbol = irProperty.symbol
        delegateVariableForPropertyCache[symbol] = irProperty.delegate.symbol
        getterForPropertyCache[symbol] = irProperty.getter.symbol
        irProperty.setter?.let { setterForPropertyCache[symbol] = it.symbol }
        localStorage.putDelegatedProperty(property, irProperty)
        return irProperty
    }

    // ------------------------------------ variables ------------------------------------

    fun createAndCacheIrVariable(
        variable: FirVariable,
        irParent: IrDeclarationParent,
        givenOrigin: IrDeclarationOrigin? = null
    ): IrVariable {
        return callablesGenerator.createIrVariable(variable, irParent, givenOrigin).also {
            localStorage.putVariable(variable, it)
        }
    }

    fun getIrValueSymbol(firVariableSymbol: FirVariableSymbol<*>): IrSymbol {
        return when (val firDeclaration = firVariableSymbol.fir) {
            is FirEnumEntry -> {
                classifierStorage.getCachedIrEnumEntry(firDeclaration)?.let { return it.symbol }
                val irParentClass = firDeclaration.containingClassLookupTag()?.let { classifierStorage.findIrClass(it) }!!

                val firProviderForSymbol = firVariableSymbol.moduleData.session.firProvider
                val containingFile = firProviderForSymbol.getFirCallableContainerFile(firVariableSymbol)

                classifierStorage.getOrCreateIrEnumEntry(
                    firDeclaration,
                    irParent = irParentClass,
                    predefinedOrigin = if (containingFile != null) IrDeclarationOrigin.DEFINED else irParentClass.origin
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

    private fun getIrVariableSymbol(firVariable: FirVariable): IrVariableSymbol {
        return localStorage.getVariable(firVariable)?.symbol
            ?: run {
                throw IllegalArgumentException("Cannot find variable ${firVariable.render()} in local storage")
            }
    }

    // ------------------------------------ anonymous initializers ------------------------------------

    fun getOrCreateIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        containingIrClass: IrClass,
    ): IrAnonymousInitializer {
        return initializerCache.computeIfAbsent(anonymousInitializer) {
            callablesGenerator.createIrAnonymousInitializer(anonymousInitializer, containingIrClass)
        }
    }

    // ------------------------------------ callables ------------------------------------

    fun originalDeclarationForDelegated(irDeclaration: IrDeclaration): FirDeclaration? {
        return delegatedReverseCache[irDeclaration]
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

    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
    ): IrFunctionSymbol {
        return when (val fir = firFunctionSymbol.fir) {
            is FirAnonymousFunction -> {
                getCachedIrFunction(fir)?.let { return it.symbol }
                val irParent = findIrParent(fir, fakeOverrideOwnerLookupTag)
                val parentOrigin = (irParent as? IrDeclaration)?.origin ?: IrDeclarationOrigin.DEFINED
                val declarationOrigin = computeDeclarationOrigin(firFunctionSymbol, parentOrigin)
                createAndCacheIrFunction(fir, irParent, predefinedOrigin = declarationOrigin).symbol
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
                        createAndCacheIrFunction(
                            fir, parent,
                            predefinedOrigin = origin,
                            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                        )
                    },
                    createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                        lazyDeclarationsGenerator.createIrLazyFunction(fir, signature, lazyParent, declarationOrigin).also {
                            cacheIrFunction(fir, it, fakeOverrideOwnerLookupTag)
                        }
                    },
                ) as IrFunctionSymbol
                if (unmatchedOwner && fakeOverrideOwnerLookupTag is ConeClassLookupTagWithFixedSymbol) {
                    @OptIn(IrSymbolInternals::class)
                    val originalFunction = originalSymbol.owner as IrSimpleFunction
                    fakeOverrideOwnerLookupTag.findIrFakeOverride(fir.name, originalFunction) as IrFunctionSymbol? ?: originalSymbol
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

    private inline fun <reified FC : FirCallableDeclaration, reified IC : IrDeclaration> getCachedIrCallable(
        declaration: FC,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        cache: MutableMap<FC, IC>,
        signatureCalculator: () -> IdSignature?,
        referenceIfAny: (IdSignature) -> IC?
    ): IC? {
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
        val isFakeOverride = declaration.isFakeOverride(fakeOverrideOwnerLookupTag)
        if (isFakeOverride) {
            val key = FakeOverrideIdentifier(
                declaration.unwrapFakeOverrides().symbol,
                fakeOverrideOwnerLookupTag ?: declaration.containingClassLookupTag()!!
            )
            irFakeOverridesForFirFakeOverrideMap[key]?.let { return it as IC }
        } else {
            cache[declaration]?.let { return it }
        }

        // TODO: Special case mentioned above. Should be removed after fixing creation. KT-61085
        if (declaration.isSubstitutionOrIntersectionOverride) {
            cache[declaration]?.let { return it }
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
                    irFakeOverridesForFirFakeOverrideMap[key] = cachedInSymbolTable
                }
                configuration.useIrFakeOverrideBuilder -> {
                    /*
                     * If IR fake override builder is used for building fake-overrides, they are generated bypassing Fir2IrDeclarationStorage,
                     *   and are written directly to SymbolTable. So in this case it is normal to save the result from symbol table into
                     *   storage
                     *
                     * TODO: potentially this situation won't happen after migration from FIR2IR f/o generator to IR f/o generator (see KT-58861)
                     */
                    cache[declaration] = cachedInSymbolTable
                }
                declaration.initialSignatureAttr != null -> {
                    /*
                     * FIR creates remapped functions for builtin JVM classes based on use-site session, not declaration site
                     * It leads to the situations when we have two different mapped FIR functions for the same original function
                     *   (and same IR function)
                     */
                    cache[declaration] = cachedInSymbolTable
                }
                declaration.symbol is FirJavaOverriddenSyntheticPropertySymbol -> {
                    /*
                     * Synthetic properties for java classes, if those properties are based on real Kotlin properties are also session
                     *   dependant
                     */
                    cache[declaration] = cachedInSymbolTable
                }
                declaration.origin.generatedAnyMethod -> {
                    /*
                     * Generated methods from Any for data and value classes are session-dependant
                     */
                    cache[declaration] = cachedInSymbolTable
                }
                else -> {
                    error("IR declaration with signature \"$signature\" found in SymbolTable and not found in declaration storage")
                }
            }
            return cachedInSymbolTable
        }

        return null
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
    ): IrSymbol {
        val fir = firSymbol.fir as F
        val irParent by lazy { findIrParent(fir, fakeOverrideOwnerLookupTag) }
        val signature by lazy {
            signatureComposer.composeSignature(
                fir,
                fakeOverrideOwnerLookupTag,
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
            when (val parent = irParent) {
                is Fir2IrLazyClass -> {
                    assert(parentOrigin != IrDeclarationOrigin.DEFINED || configuration.allowNonCachedDeclarations) {
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
                callablesGenerator.setAndModifyParent((this as IrDeclaration), irParent)
            }.symbol
        }
    }

    // ------------------------------------ scripts ------------------------------------

    fun getCachedIrScript(script: FirScript): IrScript? {
        return scriptCache[script]
    }

    fun getOrCreateIrScript(script: FirScript): IrScript {
        getCachedIrScript(script)?.let { return it }
        return callablesGenerator.createIrScript(script).also {
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

        val parentPackage = when (firBasedSymbol) {
            is FirCallableSymbol<*> -> {
                getIrExternalPackageFragment(packageFqName, firOrigin)
            }
            else -> {
                // TODO: All classes from BUILT_INS_PACKAGE_FQ_NAMES are considered built-ins now,
                // which is not exact and can lead to some problems
                getIrExternalOrBuiltInsPackageFragment(packageFqName, firOrigin)
            }
        }

        val firProviderForSymbol = firBasedSymbol.moduleData.session.firProvider
        val containerFile = when (firBasedSymbol) {
            is FirCallableSymbol -> firProviderForSymbol.getFirCallableContainerFile(firBasedSymbol)
            is FirClassLikeSymbol -> firProviderForSymbol.getFirClassifierContainerFileIfAny(firBasedSymbol)
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

    private inline fun <reified S : IrSymbol, reified D : IrOverridableDeclaration<S>> ConeClassLookupTagWithFixedSymbol.findIrFakeOverride(
        name: Name, originalDeclaration: IrOverridableDeclaration<S>
    ): IrSymbol? {
        val dispatchReceiverIrClass =
            classifierStorage.getOrCreateIrClass(toSymbol(session) as FirClassSymbol)
        return dispatchReceiverIrClass.declarations.find {
            it is D && it.isFakeOverride && it.name == name && it.overrides(originalDeclaration)
        }?.symbol
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

    companion object {
        internal val ENUM_SYNTHETIC_NAMES = mapOf(
            Name.identifier("values") to IrSyntheticBodyKind.ENUM_VALUES,
            Name.identifier("valueOf") to IrSyntheticBodyKind.ENUM_VALUEOF,
            Name.identifier("entries") to IrSyntheticBodyKind.ENUM_ENTRIES,
            Name.special("<get-entries>") to IrSyntheticBodyKind.ENUM_ENTRIES
        )
    }
}

internal fun FirCallableDeclaration.isFakeOverride(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
    if (isSubstitutionOrIntersectionOverride) return true
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
