/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.putToMultiMap

fun IrSymbol.kind(): BinarySymbolData.SymbolKind {
    return when (this) {
        is IrClassSymbol -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol -> BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL
        is IrSimpleFunctionSymbol -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
        is IrPropertySymbol -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        is IrFieldSymbol -> BinarySymbolData.SymbolKind.FIELD_SYMBOL
        is IrEnumEntrySymbol -> BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL
        is IrTypeAliasSymbol -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        is IrTypeParameterSymbol -> BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL
        else -> error("Unexpected symbol kind $this")
    }
}

// TODO: drop CompatibilityMode, KT-72100
class CompatibilityMode(val abiVersion: KotlinAbiVersion) {

    init {
        require(abiVersion.isCompatible()) {
            "Incompatible KLIB should have been already discarded in KlibLoader"
        }
    }

    /** See comments for [LAST_WITH_LEGACY_SIGNATURES_FOR_PRIVATE_AND_LOCAL_DECLARATIONS]. */
    val legacySignaturesForPrivateAndLocalDeclarations: Boolean
        get() = abiVersion.isAtMost(LAST_WITH_LEGACY_SIGNATURES_FOR_PRIVATE_AND_LOCAL_DECLARATIONS)

    /** See comments for [LAST_WITH_SWAPPED_KPROPERTY2_TYPE_PARAMETER_ORDER]. */
    val swappedKProperty2TypeParameterOrder: Boolean
        get() = abiVersion.isAtMost(LAST_WITH_SWAPPED_KPROPERTY2_TYPE_PARAMETER_ORDER)

    companion object {
        val CURRENT = CompatibilityMode(KotlinAbiVersion.CURRENT)

        /**
         * KLIBs with ABI version <= 1.5.0 had different rules for computing signatures for private and local declarations.
         * See also [org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrExportCheckerVisitor.CompatibleChecker].
         */
        val LAST_WITH_LEGACY_SIGNATURES_FOR_PRIVATE_AND_LOCAL_DECLARATIONS = KotlinAbiVersion(1, 5, 0)

        /**
         * KLIBs with ABI version <= 1.201.0 (Kotlin <= 2.1.x) had swapped type parameter order for
         * `KProperty2` and `KMutableProperty2` in delegated member extension properties:
         * `KProperty2<ExtensionReceiver, DispatchReceiver, Value>` instead of
         * `KProperty2<DispatchReceiver, ExtensionReceiver, Value>`.
         * Fixed in KT-75112, which landed in Kotlin 2.2.0 (ABI version 2.2.0).
         */
        val LAST_WITH_SWAPPED_KPROPERTY2_TYPE_PARAMETER_ORDER = KotlinAbiVersion(1, 201, 0)
    }
}

abstract class IrModuleDeserializer(
    val moduleFragment: IrModuleFragment,
    val libraryAbiVersion: KotlinAbiVersion,
) {
    /**
     * All package FQ names of all declarations defined in this module.
     *
     * May include false positives.
     * Does not need to include parent package names. I.e. if this module contains some declarations under `foo.bar` package,
     * but no declaration under just `foo` package, then `foo` does not need to be included in this set.
     *
     * Null value means that the set of packages is unknown (or not feasible to be provided).
     * In that case the module may be probed for every sought signature, just in case.
     */
    abstract val contributedPackageNames: Set<FqName>?

    /**
     * Looks up the symbol with the given [SignatureAndKind.signature] and [SignatureAndKind.kind] in this module and schedules
     * the corresponding declaration for deserialization.
     *
     * The declaration is not necessarily deserialized by this call, so the returned symbol may be unbound.
     * It becomes bound when the deserialization queue of this module is processed, see [deserializeReachableDeclarations].
     *
     * @return The symbol, or `null` if this module does not contain a declaration with the given [signatureAndKind].
     */
    @IrLinkerInternalApi
    abstract fun findAndEnqueueSymbolOrNull(signatureAndKind: SignatureAndKind): IrSymbol?

    abstract operator fun contains(idSig: IdSignature): Boolean
    abstract fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol?

    abstract val klib: KotlinLibrary

    open fun postProcess() {}

    /**
     * Schedule deserialization of the top-level declaration with the given signature in the given file.
     */
    open fun addModuleReachableTopLevel(topLevelDeclarationSignature: IdSignature) {
        error("Unsupported Operation (sig: $topLevelDeclarationSignature")
    }

    /**
     * Run deserialization of top-level declarations previously scheduled for deserialization in the current module.
     */
    open fun deserializeReachableDeclarations() { error("Unsupported Operation") }

    open val strategyResolver: DeserializationStrategyResolver = DeserializationStrategy.ONLY_REFERENCED_DECLARATION_HEADERS

    open val fileDeserializers: Map<IrFile, IrFileDeserializer> get() = emptyMap()

    val compatibilityMode: CompatibilityMode get() = CompatibilityMode(libraryAbiVersion)

    /**
     * Collects signatures of all top-level callables matching the given [topLevelCallableId]
     * that are defined in this module, and adds them to [output].
     *
     * **This is an internal API of the linker.** It is only supposed to be called by [KotlinIrLinker]
     * and overridden by implementations of [IrModuleDeserializer]. Everyone else should use
     * [KotlinIrLinker.getAllMatchingSignatures] instead.
     *
     * If there are no matching callables in this module, [output] is left unchanged.
     */
    @IrLinkerInternalApi
    abstract fun collectAllMatchingSignatures(
        topLevelCallableId: TopLevelCallableIdAndKind,
        output: MutableCollection<IdSignature>,
    )
}

// Used to resolve built in symbols like `kotlin.ir.internal.*` or `kotlin.FunctionN`
@OptIn(IrLinkerInternalApi::class)
class IrModuleDeserializerWithBuiltIns(
    moduleFragment: IrModuleFragment,
    private val symbolTable: SymbolTable,
    mangler: KotlinMangler.IrMangler,
    onDeserializedClass: (IrClass, IdSignature) -> Unit,
    val delegate: IrModuleDeserializer
) : IrModuleDeserializer(moduleFragment, delegate.libraryAbiVersion) {
    private val signatureComputer = PublicIdSignatureComputer(mangler)
    private val syntheticProvider = IrSyntheticProvider(
        module = moduleFragment,
        symbolTable = symbolTable,
        signatureComputer = signatureComputer::computeSignature
    )

    private val syntheticFunctionClassGenerator = run {
        IrBasedFunctionFactory(
            moduleFragment,
            symbolTable.referenceClass(StandardClassIds.Function.toIdSignature()),
            symbolTable.referenceClass(StandardClassIds.KFunction.toIdSignature()),
            symbolTable.referenceClass(StandardClassIds.Any.toIdSignature()),
            symbolTable,
            signatureComputer::computeSignature,
            onDeserializedClass
        )
    }

    private val irBuiltInsMap: Map<SignatureAndKind, IrSymbol> = syntheticProvider.operatorsPackageFragment.declarations.associate {
        val symbol = (it as IrSymbolOwner).symbol
        val signature = symbol.signature!!
        SignatureAndKind(signature, symbol.kind()) to symbol
    }

    // TODO(KT-91417): Drop this legacy map after completely migrating to signature+symbolKind addressing scheme.
    private val legacyIrBuiltInsMap: Map<IdSignature, IrSymbol> = irBuiltInsMap.mapKeys { it.key.signature }

    override val contributedPackageNames: Set<FqName> = buildSet {
        this += syntheticProvider.operatorsPackageFragment.packageFqName
        delegate.contributedPackageNames?.let { addAll(it) }
    }

    private val irBuiltInsCallableIdMap: Map<TopLevelCallableIdAndKind, List<IdSignature>> by lazy {
        buildMap<TopLevelCallableIdAndKind, MutableList<IdSignature>> {
            syntheticProvider.operatorsPackageFragment.declarations.forEach {
                val symbol = (it as IrSymbolOwner).symbol
                putToMultiMap(TopLevelCallableIdAndKind(symbol), symbol.signature!!)
            }
        }
    }

    override fun findAndEnqueueSymbolOrNull(signatureAndKind: SignatureAndKind): IrSymbol? {
        irBuiltInsMap[signatureAndKind]?.let { return it }

        val [topLevelSignature, topLevelKind] = signatureAndKind.topLevel() ?: return null
        if (checkIsFunctionInterface(topLevelSignature) && topLevelKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) {
            computeFunctionClass(topLevelSignature)
            return referenceDeserializedSymbol(symbolTable, signatureAndKind)
        }

        return delegate.findAndEnqueueSymbolOrNull(signatureAndKind)
    }

    override operator fun contains(idSig: IdSignature): Boolean {
        val topLevel = idSig.topLevelSignature()
        if (topLevel in legacyIrBuiltInsMap) return true

        return checkIsFunctionInterface(topLevel) || idSig in delegate
    }

    override fun deserializeReachableDeclarations() {
        delegate.deserializeReachableDeclarations()
    }

    private fun computeFunctionClass(idSig: IdSignature): IrClass {
        val publicSig = idSig.asPublic() ?: error("$idSig has to be public")
        val fqnParts = publicSig.nameSegments
        val className = fqnParts.firstOrNull() ?: error("Expected class name for $idSig")

        val isK = className[0] == 'K'
        val isSuspend = (if (isK) className[1] else className[0]) == 'S'
        val arity = className.run { substring(indexOfFirst { it.isDigit() }).toInt(10) }
        return syntheticFunctionClassGenerator.run {
            when {
                isK && isSuspend -> kSuspendFunctionN(arity)
                isK -> kFunctionN(arity)
                isSuspend -> suspendFunctionN(arity)
                else -> functionN(arity)
            }
        }
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        irBuiltInsMap[SignatureAndKind(idSig, symbolKind)]?.let { return it }

        val topLevel = idSig.topLevelSignature()
        if (checkIsFunctionInterface(topLevel)) {
            computeFunctionClass(topLevel)
            return referenceDeserializedSymbol(symbolTable, null, symbolKind, idSig)
        }

        return delegate.tryDeserializeIrSymbol(idSig, symbolKind)
    }

    override val klib: KotlinLibrary
        get() = delegate.klib

    override val strategyResolver get() = delegate.strategyResolver

    override fun addModuleReachableTopLevel(topLevelDeclarationSignature: IdSignature) {
        delegate.addModuleReachableTopLevel(topLevelDeclarationSignature)
    }

    override val fileDeserializers: Map<IrFile, IrFileDeserializer>
        get() = delegate.fileDeserializers

    override fun postProcess() {
        delegate.postProcess()
    }

    internal fun finish(irBuiltIns: IrBuiltIns) {
        if (syntheticFunctionClassGenerator.typeSystem != null) return
        syntheticFunctionClassGenerator.typeSystem = IrTypeSystemContextImpl(irBuiltIns)
        @OptIn(UnstableBuiltInsApi::class)
        irBuiltIns.functionFactory = syntheticFunctionClassGenerator
    }

    override fun collectAllMatchingSignatures(
        topLevelCallableId: TopLevelCallableIdAndKind,
        output: MutableCollection<IdSignature>,
    ) {
        irBuiltInsCallableIdMap[topLevelCallableId]?.let { output += it }
        delegate.collectAllMatchingSignatures(topLevelCallableId, output)
    }
}
