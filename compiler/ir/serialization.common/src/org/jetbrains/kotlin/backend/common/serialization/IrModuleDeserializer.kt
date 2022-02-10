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
import org.jetbrains.kotlin.ir.util.StringSignature
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinAbiVersion

internal fun IrSymbol.kind(): BinarySymbolData.SymbolKind {
    return when (this) {
        is IrClassSymbol -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol -> BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL
        is IrSimpleFunctionSymbol -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
        is IrPropertySymbol -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        is IrEnumEntrySymbol -> BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL
        is IrTypeAliasSymbol -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        is IrTypeParameterSymbol -> BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL
        else -> error("Unexpected symbol kind $this")
    }
}

class CompatibilityMode(val abiVersion: KotlinAbiVersion) {

    init {
        assert(abiVersion.isCompatible())
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

        val WITH_PRIVATE_SIG = CompatibilityMode(LAST_PRIVATE_SIG_ABI_VERSION)
        val WITH_COMMON_SIG = CompatibilityMode(KotlinAbiVersion.CURRENT)

        val CURRENT = WITH_COMMON_SIG
    }
}

enum class IrModuleDeserializerKind {
    CURRENT, DESERIALIZED, SYNTHETIC
}

abstract class IrModuleDeserializer(private val _moduleDescriptor: ModuleDescriptor?, val libraryAbiVersion: KotlinAbiVersion) {
    abstract operator fun contains(signature: StringSignature): Boolean
    abstract fun deserializeIrSymbol(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol

    val moduleDescriptor: ModuleDescriptor get() = _moduleDescriptor ?: error("No ModuleDescriptor provided")

    open fun referenceSimpleFunctionByLocalSignature(file: IrFile, signature: StringSignature): IrSimpleFunctionSymbol =
        error("Unsupported operation")

    open fun referencePropertyByLocalSignature(file: IrFile, signature: StringSignature): IrPropertySymbol =
        error("Unsupported operation")

    open fun declareIrSymbol(symbol: IrSymbol) {
        val signature = symbol.signature
        if (signature != null) {
//            require(signature != null) { "Symbol is not public API: ${symbol.descriptor}" }
            assert(symbol.hasDescriptor)
            deserializeIrSymbol(signature, symbol.kind())
        } else {
            val descriptor = symbol.descriptor
            if (isExpect(descriptor)) return
            error("Symbol is not public API: ${symbol.descriptor}")
        }
    }

    open val klib: IrLibrary get() = error("Unsupported operation")

    open fun init() = init(this)

    open fun postProcess() {}

    open fun init(delegate: IrModuleDeserializer) {}

    open fun addModuleReachableTopLevel(signature: StringSignature) {
        error("Unsupported Operation (sig: $signature")
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

// Used to resolve built in symbols like `kotlin.ir.internal.*` or `kotlin.FunctionN`
class IrModuleDeserializerWithBuiltIns(
    private val builtIns: IrBuiltIns,
    private val delegate: IrModuleDeserializer
) : IrModuleDeserializer(delegate.moduleDescriptor, delegate.libraryAbiVersion) {

    init {
        // TODO: figure out how it should work for K/N
//        assert(builtIns.builtIns.builtInsModule === delegate.moduleDescriptor)
    }

    private val irBuiltInsMap = builtIns.knownBuiltins.map {
        val symbol = (it as IrSymbolOwner).symbol
        symbol.signature to symbol
    }.toMap()

    private fun checkIsFunctionInterface(signature: StringSignature): Boolean {
//        val publicSig = idSig.asPublic()
        return !signature.isLocal && signature.packageFqName().asString() in functionalPackages &&
                signature.declarationFqName.isNotEmpty() &&
                functionPattern.matcher(signature.declarationFqName.split('.').first()).find()
    }

    override operator fun contains(signature: StringSignature): Boolean {
        val topLevel = signature.topLevelSignature()
        if (topLevel in irBuiltInsMap) return true

        return checkIsFunctionInterface(topLevel) || signature in delegate
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, signature: StringSignature): IrSimpleFunctionSymbol =
        delegate.referenceSimpleFunctionByLocalSignature(file, signature)

    override fun referencePropertyByLocalSignature(file: IrFile, signature: StringSignature): IrPropertySymbol =
        delegate.referencePropertyByLocalSignature(file, signature)

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

    private fun resolveFunctionalInterface(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (symbolKind == BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL) {
            val containerSignature = signature.containerSignature()
            val classSymbol = resolveFunctionalInterface(containerSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL) as IrClassSymbol
            val typeParameterSig = signature.parsedSignature as StringSignature.ParsedSignature.TypeParameterSignature
            val typeParameterIndex = typeParameterSig.idx
            val typeParameter = classSymbol.owner.typeParameters[typeParameterIndex]
            return typeParameter.symbol
        }

        assert(!signature.isLocal) { "$signature has to be public" }

        val fqnParts = signature.nameSegments()
        val className = fqnParts.firstOrNull() ?: error("Expected class name for $signature")

        val functionClass = computeFunctionClass(className)

        return when (fqnParts.size) {
            1 -> functionClass.symbol.also { assert(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) }
            else -> {
                val parsed = signature.parsedSignature

                val memberName = fqnParts[1]

                if (parsed is StringSignature.ParsedSignature.FunctionSignature) {
                    parsed.extraName?.let { extra ->
                        functionClass.declarations.filterIsInstance<IrProperty>().single { it.name.asString() == memberName }
                            .let { property ->
                                property.getter?.let { g -> if (extra[0] == 'g') return g.symbol }
                                property.setter?.let { s -> if (extra[0] == 's') return s.symbol }
                                error("No accessor found for signature $signature")
                            }
                    }
                }

                return functionClass.declarations.single { it is IrDeclarationWithName && it.name.asString() == memberName }.let {
                    (it as IrSymbolOwner).symbol
                }
            }
        }
    }

    override fun deserializeIrSymbol(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        irBuiltInsMap[signature]?.let { return it }

        val topLevel = signature.topLevelSignature()

        if (checkIsFunctionInterface(topLevel)) return resolveFunctionalInterface(signature, symbolKind)

        return delegate.deserializeIrSymbol(signature, symbolKind)
    }

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

    override fun addModuleReachableTopLevel(signature: StringSignature) {
        delegate.addModuleReachableTopLevel(signature)
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
    override fun contains(signature: StringSignature): Boolean = false // TODO:

    override fun deserializeIrSymbol(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        error("Unreachable execution: there could not be back-links (sig: $signature)")
    }

    override fun declareIrSymbol(symbol: IrSymbol) {}

    override val kind get() = IrModuleDeserializerKind.CURRENT
}
