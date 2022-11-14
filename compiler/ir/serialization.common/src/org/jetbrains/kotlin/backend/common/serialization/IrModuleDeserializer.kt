/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes

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

class CompatibilityMode(val abiVersion: KotlinAbiVersion) {

    init {
        require(abiVersion.isCompatible()) {
            "Incompatible KLIB should have been discarded in ${KotlinLibraryProperResolverWithAttributes<Nothing>::libraryMatch.name}"
        }
    }

    val oldSignatures: Boolean
        get() {
            if (abiVersion.minor == LAST_PRIVATE_SIG_ABI_VERSION.minor) {
                return abiVersion.patch <= LAST_PRIVATE_SIG_ABI_VERSION.patch
            }
            return abiVersion.minor < LAST_PRIVATE_SIG_ABI_VERSION.minor
        }

    companion object {
        val LAST_PRIVATE_SIG_ABI_VERSION = KotlinAbiVersion(1, 5, 0)
        val CURRENT = CompatibilityMode(KotlinAbiVersion.CURRENT)
    }
}

enum class IrModuleDeserializerKind {
    CURRENT, DESERIALIZED, SYNTHETIC
}

abstract class IrModuleDeserializer(private val _moduleDescriptor: ModuleDescriptor?, val libraryAbiVersion: KotlinAbiVersion) {
    abstract operator fun contains(idSig: IdSignature): Boolean
    abstract fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol?
    abstract fun deserializedSymbolNotFound(idSig: IdSignature): Nothing

    val moduleDescriptor: ModuleDescriptor get() = _moduleDescriptor ?: error("No ModuleDescriptor provided")

    open fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature): IrSimpleFunctionSymbol =
        error("Unsupported operation")

    open fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        error("Unsupported operation")

    open fun declareIrSymbol(symbol: IrSymbol) {
        val signature = symbol.signature
        require(signature != null) { "Symbol is not public API: ${symbol.descriptor}" }
        assert(symbol.hasDescriptor)
        deserializeIrSymbolOrFail(signature, symbol.kind())
    }

    open val klib: IrLibrary get() = error("Unsupported operation")

    open fun init() = init(this)

    open fun postProcess() {}

    open fun init(delegate: IrModuleDeserializer) {}

    open fun addModuleReachableTopLevel(idSig: IdSignature) {
        error("Unsupported Operation (sig: $idSig")
    }

    open fun deserializeReachableDeclarations() { error("Unsupported Operation") }

    abstract val moduleFragment: IrModuleFragment

    abstract val moduleDependencies: Collection<IrModuleDeserializer>

    open val strategyResolver: (String) -> DeserializationStrategy = { DeserializationStrategy.ONLY_DECLARATION_HEADERS }

    abstract val kind: IrModuleDeserializerKind

    open fun fileDeserializers(): Collection<IrFileDeserializer> = error("Unsupported")

    val compatibilityMode: CompatibilityMode get() = CompatibilityMode(libraryAbiVersion)

    open fun signatureDeserializerForFile(fileName: String): IdSignatureDeserializer = error("Unsupported")
}

fun IrModuleDeserializer.deserializeIrSymbolOrFail(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol =
    tryDeserializeIrSymbol(idSig, symbolKind) ?: deserializedSymbolNotFound(idSig)

// Used to resolve built in symbols like `kotlin.ir.internal.*` or `kotlin.FunctionN`
class IrModuleDeserializerWithBuiltIns(
    private val builtIns: IrBuiltIns,
    private val delegate: IrModuleDeserializer
) : IrModuleDeserializer(delegate.moduleDescriptor, delegate.libraryAbiVersion) {

    init {
        // TODO: figure out how it should work for K/N
//        assert(builtIns.builtIns.builtInsModule === delegate.moduleDescriptor)
    }

    private val irBuiltInsMap = builtIns.knownBuiltins.associate {
        val symbol = (it as IrSymbolOwner).symbol
        symbol.signature to symbol
    }

    override operator fun contains(idSig: IdSignature): Boolean {
        val topLevel = idSig.topLevelSignature()
        if (topLevel in irBuiltInsMap) return true

        return checkIsFunctionInterface(topLevel) || idSig in delegate
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
        delegate.referenceSimpleFunctionByLocalSignature(file, idSignature)

    override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        delegate.referencePropertyByLocalSignature(file, idSignature)

    override fun deserializeReachableDeclarations() {
        delegate.deserializeReachableDeclarations()
    }

    private fun computeFunctionClass(className: String): IrClass {
        val isK = className[0] == 'K'
        val isSuspend = (if (isK) className[1] else className[0]) == 'S'
        val arity = className.run { substring(indexOfFirst { it.isDigit() }).toInt(10) }
        return builtIns.run {
            when {
                isK && isSuspend -> kSuspendFunctionN(arity)
                isK -> kFunctionN(arity)
                isSuspend -> suspendFunctionN(arity)
                else -> functionN(arity)
            }
        }
    }

    private fun resolveFunctionalInterface(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (symbolKind == BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL) {
            val composite = idSig as IdSignature.CompositeSignature
            val classSignature = idSig.container
            val classSymbol = resolveFunctionalInterface(classSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL) as IrClassSymbol
            val typeParameterSig = composite.inner as IdSignature.LocalSignature
            val typeParameterIndex = typeParameterSig.index()
            val typeParameter = classSymbol.owner.typeParameters[typeParameterIndex]
            return typeParameter.symbol
        }
        val publicSig = idSig.asPublic() ?: error("$idSig has to be public")

        val fqnParts = publicSig.nameSegments
        val className = fqnParts.firstOrNull() ?: error("Expected class name for $idSig")

        val functionClass = computeFunctionClass(className)

        return when (fqnParts.size) {
            1 -> functionClass.symbol.also { assert(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) }
            2 -> {
                val memberName = fqnParts[1]
                functionClass.declarations.single { it is IrDeclarationWithName && it.name.asString() == memberName }.let {
                    (it as IrSymbolOwner).symbol
                }
            }
            3 -> {
                assert(idSig is IdSignature.AccessorSignature)
                assert(symbolKind == BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)
                val propertyName = fqnParts[1]
                val accessorName = fqnParts[2]
                functionClass.declarations.filterIsInstance<IrProperty>().single { it.name.asString() == propertyName }.let { p ->
                    p.getter?.let { g -> if (g.name.asString() == accessorName) return g.symbol }
                    p.setter?.let { s -> if (s.name.asString() == accessorName) return s.symbol }
                    error("No accessor found for signature $idSig")
                }
            }
            else -> error("No member found for signature $idSig")
        }
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        irBuiltInsMap[idSig]?.let { return it }

        val topLevel = idSig.topLevelSignature()
        if (checkIsFunctionInterface(topLevel)) return resolveFunctionalInterface(idSig, symbolKind)

        return delegate.tryDeserializeIrSymbol(idSig, symbolKind)
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = delegate.deserializedSymbolNotFound(idSig)

    override fun declareIrSymbol(symbol: IrSymbol) {
        val signature = symbol.signature
        if (signature != null && checkIsFunctionInterface(signature))
            resolveFunctionalInterface(signature, symbol.kind())
        else delegate.declareIrSymbol(symbol)
    }

    override fun init() {
        delegate.init(this)
    }

    override val klib: IrLibrary
        get() = delegate.klib

    override val strategyResolver: (String) -> DeserializationStrategy
        get() = delegate.strategyResolver

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        delegate.addModuleReachableTopLevel(idSig)
    }

    override val moduleFragment: IrModuleFragment get() = delegate.moduleFragment
    override val moduleDependencies: Collection<IrModuleDeserializer> get() = delegate.moduleDependencies
    override val kind get() = delegate.kind

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return delegate.fileDeserializers()
    }

    override fun postProcess() {
        delegate.postProcess()
    }

    override fun signatureDeserializerForFile(fileName: String): IdSignatureDeserializer {
        return delegate.signatureDeserializerForFile(fileName)
    }
}

open class CurrentModuleDeserializer(
    override val moduleFragment: IrModuleFragment,
    override val moduleDependencies: Collection<IrModuleDeserializer>
) : IrModuleDeserializer(moduleFragment.descriptor, KotlinAbiVersion.CURRENT) {
    override fun contains(idSig: IdSignature): Boolean = false // TODO:

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): Nothing =
        error("Unreachable execution: there could not be back-links (sig: $idSig)")

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing =
        error("Unreachable execution: there could not be back-links (sig: $idSig)")

    override fun declareIrSymbol(symbol: IrSymbol) = Unit

    override val kind get() = IrModuleDeserializerKind.CURRENT
}
