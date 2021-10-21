/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.DelegateFactory
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.impl.*

fun JsMapping(irFactory: IrFactory) = JsMapping(JsMappingState(irFactory))

class JsMapping(val state: JsMappingState) : DefaultMapping(state) {
    val outerThisFieldSymbols = state.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val innerClassConstructors = state.newDeclarationToDeclarationMapping<IrConstructor, IrConstructor>()
    val originalInnerClassPrimaryConstructorByClass = state.newDeclarationToDeclarationMapping<IrClass, IrConstructor>()
    val secondaryConstructorToDelegate = state.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()
    val secondaryConstructorToFactory = state.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()
    val objectToGetInstanceFunction = state.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val objectToInstanceField = state.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val classToSyntheticPrimaryConstructor = state.newDeclarationToDeclarationMapping<IrClass, IrConstructor>()
    val privateMemberToCorrespondingStatic = state.newDeclarationToDeclarationMapping<IrFunction, IrSimpleFunction>()

    val constructorToInitFunction = state.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()

    val enumEntryToGetInstanceFun = state.newDeclarationToDeclarationMapping<IrEnumEntry, IrSimpleFunction>()
    val enumEntryToInstanceField = state.newDeclarationToDeclarationMapping<IrEnumEntry, IrField>()
    val enumConstructorToNewConstructor = state.newDeclarationToDeclarationMapping<IrConstructor, IrConstructor>()
    val enumClassToCorrespondingEnumEntry = state.newDeclarationToDeclarationMapping<IrClass, IrEnumEntry>()
    val enumConstructorOldToNewValueParameters = state.newDeclarationToDeclarationMapping<IrValueDeclaration, IrValueParameter>()
    val enumEntryToCorrespondingField = state.newDeclarationToDeclarationMapping<IrEnumEntry, IrField>()
    val fieldToEnumEntry = state.newDeclarationToDeclarationMapping<IrField, IrEnumEntry>()
    val enumClassToInitEntryInstancesFun = state.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()

    val suspendFunctionsToFunctionWithContinuations =
        state.newDeclarationToDeclarationMapping<IrSimpleFunction, IrSimpleFunction>()

    val suspendArityStore = state.newDeclarationToDeclarationCollectionMapping<IrClass, Collection<IrSimpleFunction>>()

    // Wasm mappings
    val wasmJsInteropFunctionToWrapper =
        state.newDeclarationToDeclarationMapping<IrSimpleFunction, IrSimpleFunction>()
}


class JsMappingState(val irFactory: IrFactory) : DelegateFactory {
    override fun <K : IrDeclaration, V : IrDeclaration> newDeclarationToDeclarationMapping(): Mapping.Delegate<K, V> {
        return JsMappingDelegate<K, V>(irFactory).also {
            allMappings += it
        }
    }

    override fun <K : IrDeclaration, V : Collection<IrDeclaration>> newDeclarationToDeclarationCollectionMapping(): Mapping.Delegate<K, V> {
        return JsMappingCollectionDelegate<K, V>(irFactory).also {
            allMappings += it
        }
    }

    private val allMappings = mutableListOf<SerializableMapping>()

    fun serializeMappings(declarations: Iterable<IrDeclaration>, symbolSerializer: (IrSymbol) -> Long): SerializedMappings {
        return SerializedMappings(allMappings.map { mapping ->
            val keys = mutableListOf<Long>()
            val values = mutableListOf<ByteArray>()
            declarations.forEach { d ->
                mapping.serializeMapping(d, symbolSerializer)?.let { bytes ->
                    keys += symbolSerializer((d as IrSymbolOwner).symbol)
                    values += bytes
                }
            }

            SerializedMapping(
                IrMemoryLongArrayWriter(keys).writeIntoMemory(),
                IrMemoryArrayWriter(values).writeIntoMemory(),
            )
        })
    }

    fun mappingsDeserializer(mapping: SerializedMappings, signatureDeserializer: (Long) -> IdSignature, symbolDeserializer: (Long) -> IrSymbol): (IdSignature, IrDeclaration) -> Unit {
        if (allMappings.size != mapping.mappings.size) error("Mapping size mismatch")

        val index = Array<Map<IdSignature, ByteArray>>(allMappings.size) { i ->
            val bytes = mapping.mappings[i]
            val s = IrLongArrayMemoryReader(bytes.keys).array.map(signatureDeserializer)
            val v = IrArrayMemoryReader(bytes.values).toArray()

            if (s.size != v.size) error("Keys size != values size")

            s.withIndex().associate { it.value to v[it.index] }
        }

        return { signature, declaration ->
            for (i in allMappings.indices) {
                index[i][signature]?.let { bytes ->
                    allMappings[i].loadMapping(declaration, bytes, symbolDeserializer)
                }
            }
        }
    }
}

class SerializedMappings(
    val mappings: List<SerializedMapping>
)

class SerializedMapping(
    val keys: ByteArray,
    val values: ByteArray,
)

private interface SerializableMapping {
    fun serializeMapping(declaration: IrDeclaration, symbolSerializer: (IrSymbol) -> Long): ByteArray?

    fun loadMapping(declaration: IrDeclaration, mapping: ByteArray, symbolDeserializer: (Long) -> IrSymbol)
}

private class JsMappingDelegate<K : IrDeclaration, V : IrDeclaration>(val irFactory: IrFactory) : Mapping.Delegate<K, V>(), SerializableMapping {

    private val map: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override operator fun get(key: K): V? {
        irFactory.stageController.lazyLower(key)
        return map[(key as IrSymbolOwner).symbol]?.owner as? V
    }

    override operator fun set(key: K, value: V?) {
        irFactory.stageController.lazyLower(key)
        if (value == null) {
            map.remove((key as IrSymbolOwner).symbol)
        } else {
            map[(key as IrSymbolOwner).symbol] = (value as IrSymbolOwner).symbol
        }
    }

    override fun serializeMapping(declaration: IrDeclaration, symbolSerializer: (IrSymbol) -> Long): ByteArray? {
        return map[(declaration as IrSymbolOwner).symbol]?.let { symbol ->
            symbolSerializer(symbol).toByteArray()
        }
    }

    override fun loadMapping(declaration: IrDeclaration, mapping: ByteArray, symbolDeserializer: (Long) -> IrSymbol) {
        map[(declaration as IrSymbolOwner).symbol] = symbolDeserializer(mapping.toLong())
    }

    override val keys: Set<K>
        get() = TODO("Not yet implemented")

    override val values: Collection<V>
        get() = TODO("Not yet implemented")
}

private class JsMappingCollectionDelegate<K : IrDeclaration, V : Collection<IrDeclaration>>(val irFactory: IrFactory) : Mapping.Delegate<K, V>(), SerializableMapping {
    private val map: MutableMap<IrSymbol, Collection<IrSymbol>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override operator fun get(key: K): V? {
        irFactory.stageController.lazyLower(key)
        return map[(key as IrSymbolOwner).symbol]?.map { it.owner as IrDeclaration } as? V
    }

    override operator fun set(key: K, value: V?) {
        irFactory.stageController.lazyLower(key)
        if (value == null) {
            map.remove((key as IrSymbolOwner).symbol)
        } else {
            map[(key as IrSymbolOwner).symbol] = value.map { (it as IrSymbolOwner).symbol }
        }
    }

    override fun serializeMapping(declaration: IrDeclaration, symbolSerializer: (IrSymbol) -> Long): ByteArray? {
        return map[(declaration as IrSymbolOwner).symbol]?.let { symbols ->
            IrMemoryLongArrayWriter(symbols.map(symbolSerializer)).writeIntoMemory()
        }
    }

    override fun loadMapping(declaration: IrDeclaration, mapping: ByteArray, symbolDeserializer: (Long) -> IrSymbol) {
        map[(declaration as IrSymbolOwner).symbol] = IrLongArrayMemoryReader(mapping).array.map(symbolDeserializer)
    }

    override val keys: Set<K>
        get() = TODO("Not yet implemented")

    override val values: Collection<V>
        get() = TODO("Not yet implemented")
}

fun ByteArray.toLong(): Long {
    var result = this[0].toLong() and 0xFFL
    for (i in 1..7) {
        result = (result shl 8) or (this[i].toLong() and 0xFFL)
    }
    return result
}

fun Long.toByteArray(): ByteArray {
    val result = ByteArray(8)

    var self = this

    for (i in 7 downTo 0) {
        result[i] = self.toByte()
        self = self ushr 8
    }

    return result
}