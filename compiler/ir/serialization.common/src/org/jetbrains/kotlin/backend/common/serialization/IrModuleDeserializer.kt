/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.IrLibrary

internal fun IrSymbol.kind(): BinarySymbolData.SymbolKind {
    return when (this) {
        is IrClassSymbol -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
        is IrConstructorSymbol -> BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL
        is IrSimpleFunctionSymbol -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
        is IrPropertySymbol -> BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        is IrEnumEntrySymbol -> BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL
        is IrTypeAliasSymbol -> BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL
        else -> error("Unexpected symbol kind $this")
    }
}

abstract class IrModuleDeserializer(val moduleDescriptor: ModuleDescriptor) {
    abstract operator fun contains(idSig: IdSignature): Boolean
    abstract fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol

    open fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
        error("Unsupported operation")

    open fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        error("Unsupported operation")

    open fun declareIrSymbol(symbol: IrSymbol) {
        val signature = symbol.signature
        require(signature != null) { "Symbol is not public API: ${symbol.descriptor}" }
        assert(symbol.hasDescriptor)
        deserializeIrSymbol(signature, symbol.kind())
    }

    open val klib: IrLibrary get() = error("Unsupported operation")

    open fun init() = init(this)

    open fun postProcess() {}

    open fun init(delegate: IrModuleDeserializer) {}

    open fun addModuleReachableTopLevel(idSig: IdSignature) { error("Unsupported Operation (sig: $idSig") }

    open fun deserializeReachableDeclarations() { error("Unsupported Operation") }

    abstract val moduleFragment: IrModuleFragment

    abstract val moduleDependencies: Collection<IrModuleDeserializer>

    open val strategy: DeserializationStrategy = DeserializationStrategy.ONLY_DECLARATION_HEADERS

    open val isCurrent = false

    open fun fileDeserializers(): Collection<IrFileDeserializer> = error("Unsupported")
}

// Used to resolve built in symbols like `kotlin.ir.internal.*` or `kotlin.FunctionN`
open class IrModuleDeserializerWithBuiltIns(
    builtIns: IrBuiltIns,
    private val functionFactory: IrAbstractFunctionFactory,
    private val delegate: IrModuleDeserializer
) : IrModuleDeserializer(delegate.moduleDescriptor) {

    init {
        // TODO: figure out how it should work for K/N
//        assert(builtIns.builtIns.builtInsModule === delegate.moduleDescriptor)
    }

    private val irBuiltInsMap = builtIns.knownBuiltins.map {
        val symbol = (it as IrSymbolOwner).symbol
        symbol.signature to symbol
    }.toMap() + additionalBuiltIns(builtIns)

    protected open fun additionalBuiltIns(builtIns: IrBuiltIns): Map<IdSignature, IrSymbol> = emptyMap()

    protected open fun checkIsFunctionInterface(idSig: IdSignature): Boolean {
        val publicSig = idSig.asPublic()
        return publicSig != null &&
                publicSig.packageFqName in functionalPackages &&
                publicSig.declarationFqName.isNotEmpty() &&
                functionPattern.matcher(publicSig.firstNameSegment).find()
    }

    override operator fun contains(idSig: IdSignature): Boolean {
        if (idSig in irBuiltInsMap) return true

        return checkIsFunctionInterface(idSig) || idSig in delegate
    }

    override fun referenceSimpleFunctionByLocalSignature(file: IrFile, idSignature: IdSignature) : IrSimpleFunctionSymbol =
        delegate.referenceSimpleFunctionByLocalSignature(file, idSignature)

    override fun referencePropertyByLocalSignature(file: IrFile, idSignature: IdSignature): IrPropertySymbol =
        delegate.referencePropertyByLocalSignature(file, idSignature)

    override fun deserializeReachableDeclarations() {
        delegate.deserializeReachableDeclarations()
    }

    private fun computeFunctionDescriptor(className: String): FunctionClassDescriptor {
        val isK = className[0] == 'K'
        val isSuspend = (if (isK) className[1] else className[0]) == 'S'
        val arity = className.run { substring(indexOfFirst { it.isDigit() }).toInt(10) }
        return functionFactory.run {
            when {
                isK && isSuspend -> kSuspendFunctionClassDescriptor(arity)
                isK -> kFunctionClassDescriptor(arity)
                isSuspend -> suspendFunctionClassDescriptor(arity)
                else -> functionClassDescriptor(arity)
            }
        }
    }

    protected open fun resolveFunctionalInterface(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        val publicSig = idSig.asPublic() ?: error("$idSig has to be public")

        val fqnParts = publicSig.nameSegments
        val className = fqnParts.firstOrNull() ?: error("Expected class name for $idSig")

        val functionDescriptor = computeFunctionDescriptor(className)
        val topLevelSignature = IdSignature.PublicSignature(publicSig.packageFqName, className, null, publicSig.mask)

        val functionClass = when (functionDescriptor.functionKind) {
            FunctionClassKind.KSuspendFunction -> functionFactory.kSuspendFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassKind.KFunction -> functionFactory.kFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassKind.SuspendFunction -> functionFactory.suspendFunctionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
            FunctionClassKind.Function -> functionFactory.functionN(functionDescriptor.arity) { callback ->
                declareClassFromLinker(functionDescriptor, topLevelSignature) { callback(it) }
            }
        }

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

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        irBuiltInsMap[idSig]?.let { return it }

        if (checkIsFunctionInterface(idSig)) return resolveFunctionalInterface(idSig, symbolKind)

        return delegate.deserializeIrSymbol(idSig, symbolKind)
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

    override val strategy: DeserializationStrategy
        get() = delegate.strategy

    override fun addModuleReachableTopLevel(idSig: IdSignature) {
        delegate.addModuleReachableTopLevel(idSig)
    }

    override val moduleFragment: IrModuleFragment get() = delegate.moduleFragment
    override val moduleDependencies: Collection<IrModuleDeserializer> get() = delegate.moduleDependencies
    override val isCurrent get() = delegate.isCurrent

    override fun fileDeserializers(): Collection<IrFileDeserializer> {
        return delegate.fileDeserializers()
    }

    override fun postProcess() {
        delegate.postProcess()
    }
}

open class CurrentModuleDeserializer(
    override val moduleFragment: IrModuleFragment,
    override val moduleDependencies: Collection<IrModuleDeserializer>
) : IrModuleDeserializer(moduleFragment.descriptor) {
    override fun contains(idSig: IdSignature): Boolean = false // TODO:

    override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        error("Unreachable execution: there could not be back-links (sig: $idSig)")
    }

    override fun declareIrSymbol(symbol: IrSymbol) {}

    override val isCurrent = true
}
