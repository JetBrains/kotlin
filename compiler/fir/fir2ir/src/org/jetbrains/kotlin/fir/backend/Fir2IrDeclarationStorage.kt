/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.generateOverriddenAccessorSymbols
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.descriptors.FirBuiltInsPackageFragment
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
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
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.threadLocal
import java.util.concurrent.ConcurrentHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
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

    @OptIn(IrSymbolInternals::class)
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
        val scope = firClass.unsubstitutedScope()
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

    private fun FirTypeRef.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeOrigin) }

    private fun ConeKotlinType.toIrType(typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT): IrType =
        with(typeConverter) { toIrType(typeOrigin) }

    private fun getIrExternalOrBuiltInsPackageFragment(fqName: FqName, firOrigin: FirDeclarationOrigin): IrExternalPackageFragment {
        val isBuiltIn = fqName in BUILT_INS_PACKAGE_FQ_NAMES
        return if (isBuiltIn) getIrBuiltInsPackageFragment(fqName) else getIrExternalPackageFragment(fqName, firOrigin)
    }

    private fun getIrBuiltInsPackageFragment(fqName: FqName): IrExternalPackageFragment {
        return builtInsFragmentCache.getOrPut(fqName) {
            createExternalPackageFragment(FirBuiltInsPackageFragment(fqName, moduleDescriptor))
        }
    }

    fun getIrExternalPackageFragment(
        fqName: FqName,
        firOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): IrExternalPackageFragment {
        val fragments = fragmentCache.getOrPut(fqName) {
            ExternalPackageFragments(
                fragmentForDependencies = createExternalPackageFragment(fqName, FirModuleDescriptor(session, moduleDescriptor.builtIns)),
                fragmentForPrecompiledBinaries = createExternalPackageFragment(fqName, moduleDescriptor)
            )
        }
        // Make sure that external package fragments have a different module descriptor. The module descriptors are compared
        // to determine if objects need regeneration because they are from different modules.
        // But keep original module descriptor for the fragments coming from parts compiled on the previous incremental step
        return when (firOrigin) {
            FirDeclarationOrigin.Precompiled -> fragments.fragmentForPrecompiledBinaries
            else -> fragments.fragmentForDependencies
        }
    }

    private fun createExternalPackageFragment(fqName: FqName, moduleDescriptor: FirModuleDescriptor): IrExternalPackageFragment {
        return createExternalPackageFragment(FirPackageFragmentDescriptor(fqName, moduleDescriptor))
    }

    private fun createExternalPackageFragment(packageFragmentDescriptor: PackageFragmentDescriptor): IrExternalPackageFragment {
        val symbol = IrExternalPackageFragmentSymbolImpl(packageFragmentDescriptor)
        return IrExternalPackageFragmentImpl(symbol, packageFragmentDescriptor.fqName)
    }

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

    private class NonCachedSourceFileFacadeClass(
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

    /**
     * [firCallable] is function or property (if [irFunction] is a property accessor) for
     *   which [irFunction] was build
     *
     * It is needed to determine proper dispatch receiver type if this declaration is fake-override
     */
    private fun computeDispatchReceiverType(
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
          *   class from step 2
         */
        if (firCallable is FirProperty && firCallable.isLocal) return null
        val containingClass = computeContainingClass(parent) ?: return null
        val defaultType = containingClass.defaultType
        if (firCallable == null) return defaultType
        if (irFunction.origin != IrDeclarationOrigin.FAKE_OVERRIDE) return defaultType

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
        return if (parent is IrClass && parent !is NonCachedSourceFileFacadeClass) {
            parent
        } else {
            null
        }
    }

    private fun findIrParent(callableDeclaration: FirCallableDeclaration): IrDeclarationParent? {
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
            with(classifierStorage) {
                setTypeParameters(function)
            }
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
                    createIrParameter(
                        valueParameter, index + contextReceiverParametersCount,
                        useStubForDefaultValueStub = function !is FirConstructor || containingClass?.name != Name.identifier("Enum"),
                        typeOrigin,
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
                        thisType = receiver.typeRef.toIrType(typeOrigin),
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

    fun <T : IrFunction> T.putParametersInScope(function: FirFunction): T {
        val contextReceivers = function.contextReceiversForFunctionOrContainingProperty()

        for ((firParameter, irParameter) in function.valueParameters.zip(valueParameters.drop(contextReceivers.size))) {
            localStorage.putParameter(firParameter, irParameter)
        }
        return this
    }

    fun getCachedIrFunction(function: FirFunction): IrSimpleFunction? {
        return if (function is FirSimpleFunction) getCachedIrFunction(function)
        else localStorage.getLocalFunction(function)
    }

    fun getCachedIrFunction(function: FirSimpleFunction): IrSimpleFunction? {
        return getCachedIrFunction(function, fakeOverrideOwnerLookupTag = null) { signatureComposer.composeSignature(function) }
    }

    @OptIn(IrSymbolInternals::class)
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
            symbolTable.referenceSimpleFunctionIfAny(signature)?.owner
        }
        return cachedIrCallable
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
    ): IrSimpleFunction {
        getCachedIrFunction(function)?.let { return it }
        return createIrFunction(
            function,
            irParent,
            isLocal = isLocal,
            fakeOverrideOwnerLookupTag = function.containingClassLookupTag()
        )
    }

    fun createIrFunction(
        function: FirFunction,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
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
                signatureComposer.composeSignature(function, fakeOverrideOwnerLookupTag)
            }
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy function
            return createIrLazyFunction(function as FirSimpleFunction, signature, irParent, updatedOrigin)
        }
        val name = simpleFunction?.name
            ?: if (isLambda) SpecialNames.ANONYMOUS else SpecialNames.NO_NAME_PROVIDED
        val visibility = simpleFunction?.visibility ?: Visibilities.Local
        val isSuspend =
            if (isLambda) ((function as FirAnonymousFunction).typeRef as? FirResolvedTypeRef)?.type?.isSuspendOrKSuspendFunctionType(session) == true
            else function.isSuspend
        val created = function.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrSimpleFunction(signature) { symbol ->
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
                    enterScope(this.symbol)
                    setAndModifyParent(irParent)
                    declareParameters(
                        function, irParent,
                        dispatchReceiverType = computeDispatchReceiverType(this, simpleFunction, irParent),
                        isStatic = simpleFunction?.isStatic == true,
                        forSetter = false,
                    )
                    convertAnnotationsForNonDeclaredMembers(function, origin)
                    leaveScope(this.symbol)
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
        if (function.isFakeOverride(fakeOverrideOwnerLookupTag)) {
            val originalFunction = function.unwrapFakeOverrides()
            val key = FakeOverrideIdentifier(
                originalFunction.symbol,
                fakeOverrideOwnerLookupTag ?: function.containingClassLookupTag()!!
            )
            irFakeOverridesForFirFakeOverrideMap[key] = created
        } else {
            functionCache[function] = created
        }
        return created
    }

    fun getCachedIrAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer): IrAnonymousInitializer? =
        initializerCache[anonymousInitializer]

    fun createIrAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        irParent: IrClass
    ): IrAnonymousInitializer = convertCatching(anonymousInitializer) {
        return anonymousInitializer.convertWithOffsets { startOffset, endOffset ->
            symbolTable.descriptorExtension.declareAnonymousInitializer(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                irParent.descriptor
            ).apply {
                this.parent = irParent
                initializerCache[anonymousInitializer] = this
            }
        }
    }

    @OptIn(IrSymbolInternals::class)
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
    ): IrConstructor = convertCatching(constructor) {
        val origin = constructor.computeIrOrigin(predefinedOrigin)
        val isPrimary = constructor.isPrimary
        val signature =
            runUnless(isLocal || !configuration.linkViaSignatures) {
                signatureComposer.composeSignature(constructor)
            }
        val visibility = if (irParent.isAnonymousObject) Visibilities.Public else constructor.visibility
        return constructor.convertWithOffsets { startOffset, endOffset ->
            declareIrConstructor(signature) { symbol ->
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
                    // Add to cache before generating parameters to prevent an infinite loop when an annotation value parameter is annotated
                    // with the annotation itself.
                    constructorCache[constructor] = this
                    enterScope(this.symbol)
                    setAndModifyParent(irParent)
                    declareParameters(constructor, irParent, dispatchReceiverType = null, isStatic = false, forSetter = false)
                    leaveScope(this.symbol)
                }
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
        return createIrConstructor(constructor, irParent, predefinedOrigin, isLocal)
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
        isSetter: Boolean,
        origin: IrDeclarationOrigin,
        startOffset: Int,
        endOffset: Int,
        dontUseSignature: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
        propertyAccessorForAnnotations: FirPropertyAccessor? = propertyAccessor,
    ): IrSimpleFunction = convertCatching(propertyAccessor ?: property) {
        val prefix = if (isSetter) "set" else "get"
        val signature =
            runUnless(dontUseSignature) {
                signatureComposer.composeAccessorSignature(property, isSetter, fakeOverrideOwnerLookupTag)
            }
        val containerSource = (correspondingProperty as? IrProperty)?.containerSource
        return declareIrAccessor(
            signature
        ) { symbol ->
            val accessorReturnType = if (isSetter) irBuiltIns.unitType else propertyType
            val visibility = propertyAccessor?.visibility?.let {
                components.visibilityConverter.convertToDescriptorVisibility(it)
            }
            irFactory.createSimpleFunction(
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
                with(classifierStorage) {
                    setTypeParameters(
                        property, if (isSetter) ConversionTypeOrigin.SETTER else ConversionTypeOrigin.DEFAULT
                    )
                }
                // NB: we should enter accessor' scope before declaring its parameters
                // (both setter default and receiver ones, if any)
                enterScope(this.symbol)
                if (propertyAccessor == null && isSetter) {
                    declareDefaultSetterParameter(
                        property.returnTypeRef.toIrType(ConversionTypeOrigin.SETTER),
                        firValueParameter = null
                    )
                }
                setAndModifyParent(irParent)
                val dispatchReceiverType = computeDispatchReceiverType(this, property, irParent)
                declareParameters(
                    propertyAccessor, irParent, dispatchReceiverType,
                    isStatic = irParent !is IrClass || propertyAccessor?.isStatic == true, forSetter = isSetter,
                    parentPropertyReceiver = property.receiverParameter,
                )
                leaveScope(this.symbol)
                if (correspondingProperty is Fir2IrLazyProperty && correspondingProperty.containingClass != null && !isFakeOverride && dispatchReceiverType != null) {
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
        val inferredType = type ?: firInitializerExpression!!.resolvedType.toIrType()
        return declareIrField { symbol ->
            irFactory.createField(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = name,
                visibility = visibility,
                symbol = symbol,
                type = inferredType,
                isFinal = isFinal,
                isStatic = property.isStatic || !(parent is IrClass || parent is IrScript),
                isExternal = property.isExternal,
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
            origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty -> status.visibility
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

    fun findGetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol {
        return getterForPropertyCache.getValue(propertySymbol)
    }

    fun findSetterOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrSimpleFunctionSymbol? {
        return setterForPropertyCache[propertySymbol]
    }

    fun findDelegateVariableOfProperty(propertySymbol: IrLocalDelegatedPropertySymbol): IrVariableSymbol {
        return delegateVariableForPropertyCache.getValue(propertySymbol)
    }

    fun createIrProperty(
        property: FirProperty,
        irParent: IrDeclarationParent?,
        predefinedOrigin: IrDeclarationOrigin? = null,
        isLocal: Boolean = false,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
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
                signatureComposer.composeSignature(property, fakeOverrideOwnerLookupTag)
            }
        if (irParent is Fir2IrLazyClass && signature != null) {
            // For private functions signature is null, fallback to non-lazy property
            return createIrLazyProperty(property, signature, irParent, origin)
        }
        return property.convertWithOffsets { startOffset, endOffset ->
            val result = declareIrProperty(signature) { symbol ->
                classifierStorage.preCacheTypeParameters(property, symbol)
                irFactory.createProperty(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = origin,
                    name = property.name,
                    visibility = components.visibilityConverter.convertToDescriptorVisibility(property.visibility),
                    modality = property.modality!!,
                    symbol = symbol,
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
                    enterScope(this.symbol)
                    if (irParent != null) {
                        parent = irParent
                    }
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
                                property, IrDeclarationOrigin.PROPERTY_DELEGATE,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                NameUtils.propertyDelegateName(property.name), true, delegate
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
                                property, IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                                components.visibilityConverter.convertToDescriptorVisibility(property.fieldVisibility),
                                property.name, property.isVal, initializer, typeToUse
                            ).also { field ->
                                if (initializer is FirConstExpression<*>) {
                                    val constType = initializer.resolvedType.toIrType()
                                    field.initializer = factory.createExpressionBody(initializer.toIrConst(constType))
                                }
                            }
                        }
                        backingField.symbol.let {
                            backingFieldForPropertyCache[symbol] = it
                            propertyForBackingFieldCache[it] = symbol
                        }
                        this.backingField = backingField
                    }
                    if (irParent != null) {
                        backingField?.parent = irParent
                    }
                    this.getter = createIrPropertyAccessor(
                        getter, property, this, type, irParent, false,
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
                        dontUseSignature = signature == null, fakeOverrideOwnerLookupTag,
                        property.unwrapFakeOverrides().getter,
                    ).also {
                        getterForPropertyCache[symbol] = it.symbol
                    }
                    if (property.isVar) {
                        this.setter = createIrPropertyAccessor(
                            setter, property, this, type, irParent, true,
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
                        ).also {
                            setterForPropertyCache[symbol] = it.symbol
                        }
                    }
                    leaveScope(this.symbol)
                }
            }
            if (property.isFakeOverride(fakeOverrideOwnerLookupTag)) {
                val originalProperty = property.unwrapFakeOverrides()
                val key = FakeOverrideIdentifier(
                    originalProperty.symbol,
                    fakeOverrideOwnerLookupTag ?: property.containingClassLookupTag()!!
                )
                irFakeOverridesForFirFakeOverrideMap[key] = result
            } else {
                propertyCache[property] = result
            }
            result
        }
    }

    /**
     * In partial module compilation (see [org.jetbrains.kotlin.analysis.api.fir.components.KtFirCompilerFacility]),
     * referenced properties might be resolved only up to [FirResolvePhase.CONTRACTS],
     * however the backend requires the exact initializer type.
     */
    private fun getEffectivePropertyInitializer(property: FirProperty, resolveIfNeeded: Boolean): FirExpression? {
        val initializer = property.backingField?.initializer ?: property.initializer

        if (resolveIfNeeded && initializer is FirConstExpression<*>) {
            property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return getEffectivePropertyInitializer(property, resolveIfNeeded = false)
        }

        return initializer
    }

    fun getCachedIrProperty(property: FirProperty): IrProperty? {
        return getCachedIrProperty(property, fakeOverrideOwnerLookupTag = null) {
            signatureComposer.composeSignature(property)
        }
    }

    @OptIn(IrSymbolInternals::class)
    fun getCachedIrProperty(
        property: FirProperty,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?,
        signatureCalculator: () -> IdSignature?
    ): IrProperty? {
        return getCachedIrCallable(
            property,
            fakeOverrideOwnerLookupTag,
            propertyCache,
            signatureCalculator
        ) { signature ->
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
            type = initializer?.resolvedType ?: field.returnTypeRef.coneType,
            origin = IrDeclarationOrigin.DELEGATE
        ).apply {
            metadata = FirMetadataSource.Field(field)
        }
    }

    internal fun createIrField(
        field: FirField,
        irParent: IrDeclarationParent?,
        type: ConeKotlinType = field.returnTypeRef.coneType,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    ): IrField = convertCatching(field) {
        val irType = type.toIrType()
        val classId = (irParent as? IrClass)?.classId
        val containingClassLookupTag = classId?.toLookupTag()
        val signature = signatureComposer.composeSignature(field, containingClassLookupTag)
        return field.convertWithOffsets { startOffset, endOffset ->
            if (signature != null) {
                symbolTable.declareField(
                    signature, symbolFactory = { IrFieldPublicSymbolImpl(signature) }
                ) { symbol ->
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
                    )
                }
            } else {
                irFactory.createField(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = origin,
                    name = field.name,
                    visibility = components.visibilityConverter.convertToDescriptorVisibility(field.visibility),
                    symbol = IrFieldSymbolImpl(),
                    type = irType,
                    isFinal = field.modality == Modality.FINAL,
                    isStatic = field.isStatic,
                    isExternal = false
                )
            }.apply {
                metadata = FirMetadataSource.Field(field)
                val staticFakeOverrideKey = getFieldStaticFakeOverrideKey(field, containingClassLookupTag)
                if (staticFakeOverrideKey == null) {
                    fieldCache[field] = this
                } else {
                    fieldStaticOverrideCache[staticFakeOverrideKey] = this
                }
                val initializer = field.unwrapFakeOverrides().initializer
                if (initializer is FirConstExpression<*>) {
                    this.initializer = factory.createExpressionBody(initializer.toIrConst(irType))
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
        typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
        skipDefaultParameter: Boolean = false,
        // Use this parameter if you want to insert the actual default value instead of the stub (overrides useStubForDefaultValueStub parameter).
        // This parameter is intended to be used for default values of annotation parameters where they are needed and
        // may produce incorrect results for values that may be encountered outside annotations.
        // Does not do anything if valueParameter.defaultValue is already FirExpressionStub.
        forcedDefaultValueConversion: Boolean = false,
    ): IrValueParameter = convertCatching(valueParameter) {
        val origin = valueParameter.computeIrOrigin()
        val type = valueParameter.returnTypeRef.toIrType(typeOrigin)
        val irParameter = valueParameter.convertWithOffsets { startOffset, endOffset ->
            irFactory.createValueParameter(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = valueParameter.name,
                type = type,
                isAssignable = false,
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
        localStorage.putVariable(variable, irVariable)
        return irVariable
    }

    fun createIrLocalDelegatedProperty(
        property: FirProperty,
        irParent: IrDeclarationParent
    ): IrLocalDelegatedProperty = convertCatching(property) {
        val type = property.returnTypeRef.toIrType()
        val origin = IrDeclarationOrigin.DEFINED
        val symbol = IrLocalDelegatedPropertySymbolImpl()
        val irProperty = property.convertWithOffsets { startOffset, endOffset ->
            irFactory.createLocalDelegatedProperty(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = property.name,
                symbol = symbol,
                type = type,
                isVar = property.isVar
            )
        }.apply {
            parent = irParent
            metadata = FirMetadataSource.Property(property)
            enterScope(this.symbol)
            delegate = declareIrVariable(
                startOffset, endOffset, IrDeclarationOrigin.PROPERTY_DELEGATE,
                NameUtils.propertyDelegateName(property.name), property.delegate!!.resolvedType.toIrType(),
                isVar = false, isConst = false, isLateinit = false
            ).also {
                delegateVariableForPropertyCache[symbol] = it.symbol
            }
            delegate.parent = irParent
            getter = createIrPropertyAccessor(
                property.getter, property, this, type, irParent, false,
                IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, dontUseSignature = true
            ).also {
                getterForPropertyCache[symbol] = it.symbol
            }
            if (property.isVar) {
                setter = createIrPropertyAccessor(
                    property.setter, property, this, type, irParent, true,
                    IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR, startOffset, endOffset, dontUseSignature = true
                ).also {
                    setterForPropertyCache[symbol] = it.symbol
                }
            }
            annotationGenerator.generate(this, property)
            leaveScope(this.symbol)
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
            fakeOverrideOwnerLookupTag = null,
            getCachedIrDeclaration = { constructor: FirConstructor, _, calculator -> getCachedIrConstructor(constructor, calculator) },
            createIrDeclaration = { parent, origin ->
                createIrConstructor(fir, parent as IrClass, predefinedOrigin = origin)
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
        ) as IrConstructorSymbol
    }

    @OptIn(IrSymbolInternals::class)
    fun getIrFunctionSymbol(
        firFunctionSymbol: FirFunctionSymbol<*>,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
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
                            fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                        )
                    },
                    createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                        createIrLazyFunction(fir, signature, lazyParent, declarationOrigin)
                    },
                ) as IrFunctionSymbol
                if (unmatchedOwner && fakeOverrideOwnerLookupTag is ConeClassLookupTagWithFixedSymbol) {
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

    @OptIn(IrSymbolInternals::class)
    fun getIrPropertySymbol(
        firPropertySymbol: FirPropertySymbol,
        fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null,
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
                    fir, parent, predefinedOrigin = origin, fakeOverrideOwnerLookupTag = fakeOverrideOwnerLookupTag,
                )
            },
            createIrLazyDeclaration = { signature, lazyParent, declarationOrigin ->
                createIrLazyProperty(fir, signature, lazyParent, declarationOrigin)
            },
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

    @OptIn(IrSymbolInternals::class)
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
        irProperty.getter?.symbol?.let { getterForPropertyCache[symbol] = it }
        irProperty.setter?.symbol?.let { setterForPropertyCache[symbol] = it }
        irProperty.backingField?.symbol?.let {
            backingFieldForPropertyCache[symbol] = it
            propertyForBackingFieldCache[it] = symbol
        }
        return irProperty
    }

    @OptIn(IrSymbolInternals::class)
    private inline fun <reified S : IrSymbol, reified D : IrOverridableDeclaration<S>> ConeClassLookupTagWithFixedSymbol.findIrFakeOverride(
        name: Name, originalDeclaration: IrOverridableDeclaration<S>
    ): IrSymbol? {
        val dispatchReceiverIrClass =
            classifierStorage.getIrClassSymbol(toSymbol(session) as FirClassSymbol).owner
        return dispatchReceiverIrClass.declarations.find {
            it is D && it.isFakeOverride && it.name == name && it.overrides(originalDeclaration)
        }?.symbol
    }

    @OptIn(IrSymbolInternals::class)
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
    ): IrSymbol {
        val fir = firSymbol.fir as F
        val irParent by lazy { findIrParent(fir) }
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
                val irParentClass = firDeclaration.containingClassLookupTag()?.let { classifierStorage.findIrClass(it) }

                val firProviderForSymbol = firVariableSymbol.moduleData.session.firProvider
                val containingFile = firProviderForSymbol.getFirCallableContainerFile(firVariableSymbol)

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
            Name.identifier("entries") to IrSyntheticBodyKind.ENUM_ENTRIES,
            Name.special("<get-entries>") to IrSyntheticBodyKind.ENUM_ENTRIES
        )
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

    private fun FirCallableDeclaration.isFakeOverride(fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag?): Boolean {
        if (isSubstitutionOrIntersectionOverride) return true
        if (fakeOverrideOwnerLookupTag == null) return false
        // this condition is true for all places when we are trying to create "fake" fake overrides in IR
        // "fake" fake overrides are f/o which are presented in IR but have no corresponding FIR f/o
        return fakeOverrideOwnerLookupTag != containingClassLookupTag()
    }

}
