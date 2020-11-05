/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.types.Variance

class IrDeserializerForFile(
    private var annotations: List<IrConstructorCall>?,
    private val actuals: List<Actual>,
    private val fileIndex: Int,
    onlyHeaders: Boolean,
    inlineBodies: Boolean,
    deserializeFakeOverrides: Boolean,
    private val moduleDeserializer: IrModuleDeserializer,
    allowErrorNodes: Boolean,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    val fakeOverrideClassQueue: MutableList<IrClass>,
    val fakeOverrideBuilder: FakeOverrideBuilder,
    val expectUniqIdToActualUniqId: MutableMap<IdSignature, IdSignature>,
    val topLevelActualUniqItToDeserializer: MutableMap<IdSignature, IrModuleDeserializer>,
    val handleNoModuleDeserializerFound: (IdSignature) -> IrModuleDeserializer,
    val haveSeen: MutableSet<IrSymbol>,
    val referenceDeserializedSymbol: (BinarySymbolData.SymbolKind, IdSignature) -> IrSymbol,
) : IrFileDeserializer(logger, builtIns, symbolTable, !onlyHeaders, deserializeFakeOverrides, fakeOverrideClassQueue, allowErrorNodes) {

    private var fileLoops = mutableMapOf<Int, IrLoop>()

    lateinit var file: IrFile

    private val irTypeCache = mutableMapOf<Int, IrType>()

    override val deserializeInlineFunctions: Boolean = inlineBodies

    override val platformFakeOverrideClassFilter = fakeOverrideBuilder.platformSpecificClassFilter

    var reversedSignatureIndex = emptyMap<IdSignature, Int>()

    inner class FileDeserializationState {
        private val reachableTopLevels = LinkedHashSet<IdSignature>()
        val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

        fun addIdSignature(key: IdSignature) {
            reachableTopLevels.add(key)
        }

        fun processPendingDeclarations() {
            while (reachableTopLevels.isNotEmpty()) {
                val reachableKey = reachableTopLevels.first()

                val existedSymbol = deserializedSymbols[reachableKey]
                if (existedSymbol == null || !existedSymbol.isBound) {
                    val declaration = deserializeDeclaration(reachableKey)
                    file.declarations.add(declaration)
                }

                reachableTopLevels.remove(reachableKey)
            }
        }
    }

    val fileLocalDeserializationState = FileDeserializationState()

    fun deserializeDeclaration(idSig: IdSignature): IrDeclaration {
        return deserializeDeclaration(loadTopLevelDeclarationProto(idSig), file)
    }

    fun deserializeExpectActualMapping() {
        actuals.forEach {
            val expectSymbol = parseSymbolData(it.expectSymbol)
            val actualSymbol = parseSymbolData(it.actualSymbol)

            val expect = deserializeIdSignature(expectSymbol.signatureId)
            val actual = deserializeIdSignature(actualSymbol.signatureId)

            assert(expectUniqIdToActualUniqId[expect] == null) {
                "Expect signature $expect is already actualized by ${expectUniqIdToActualUniqId[expect]}, while we try to record $actual"
            }
            expectUniqIdToActualUniqId[expect] = actual
            // Non-null only for topLevel declarations.
            getModuleForTopLevelId(actual)?.let { md -> topLevelActualUniqItToDeserializer[actual] = md }
        }
    }

    private fun resolveSignatureIndex(idSig: IdSignature): Int {
        return reversedSignatureIndex[idSig] ?: error("Not found Idx for $idSig")
    }

    private fun readDeclaration(index: Int): CodedInputStream =
        moduleDeserializer.klib.irDeclaration(index, fileIndex).codedInputStream

    private fun loadTopLevelDeclarationProto(idSig: IdSignature): org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration {
        val idSigIndex = resolveSignatureIndex(idSig)
        return org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.parseFrom(readDeclaration(idSigIndex),
                                                                                               ExtensionRegistryLite.newInstance()
        )
    }

    private fun readType(index: Int): CodedInputStream =
        moduleDeserializer.klib.type(index, fileIndex).codedInputStream

    private fun loadTypeProto(index: Int): org.jetbrains.kotlin.backend.common.serialization.proto.IrType {
        return org.jetbrains.kotlin.backend.common.serialization.proto.IrType.parseFrom(readType(index),
                                                                                        ExtensionRegistryLite.newInstance()
        )
    }

    private fun readSignature(index: Int): CodedInputStream =
        moduleDeserializer.klib.signature(index, fileIndex).codedInputStream

    private fun loadSignatureProto(index: Int): org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature {
        return org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.parseFrom(readSignature(index),
                                                                                             ExtensionRegistryLite.newInstance()
        )
    }

    private fun readBody(index: Int): CodedInputStream =
        moduleDeserializer.klib.body(index, fileIndex).codedInputStream

    private fun loadStatementBodyProto(index: Int): IrStatement {
        return IrStatement.parseFrom(readBody(index), ExtensionRegistryLite.newInstance())
    }

    private fun loadExpressionBodyProto(index: Int): IrExpression {
        return IrExpression.parseFrom(readBody(index), ExtensionRegistryLite.newInstance())
    }

    private fun loadStringProto(index: Int): String {
        return String(moduleDeserializer.klib.string(index, fileIndex))
    }

    private fun getModuleForTopLevelId(idSignature: IdSignature): IrModuleDeserializer? {
        if (idSignature in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { idSignature in it }
    }

    private fun findModuleDeserializer(idSig: IdSignature): IrModuleDeserializer {
        assert(idSig.isPublic)

        val topLevelSig = idSig.topLevelSignature()
        if (topLevelSig in moduleDeserializer) return moduleDeserializer
        return moduleDeserializer.moduleDependencies.firstOrNull { topLevelSig in it } ?: handleNoModuleDeserializerFound(idSig)
    }

    private fun referenceIrSymbolData(symbol: IrSymbol, signature: IdSignature) {
        assert(signature.isLocal)
        fileLocalDeserializationState.deserializedSymbols.putIfAbsent(signature, symbol)
    }

    private fun deserializeIrLocalSymbolData(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isLocal)

        if (idSig.hasTopLevel) {
            fileLocalDeserializationState.addIdSignature(idSig.topLevelSignature())
        }

        return fileLocalDeserializationState.deserializedSymbols.getOrPut(idSig) {
            referenceDeserializedSymbol(symbolKind, idSig)
        }
    }

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) return deserializeIrLocalSymbolData(idSignature, symbolKind)

        return findModuleDeserializer(idSignature).deserializeIrSymbol(idSignature, symbolKind).also {
            haveSeen.add(it)
        }
    }

    override fun deserializeIrSymbolToDeclare(code: Long): Pair<IrSymbol, IdSignature> {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return Pair(deserializeIrSymbolData(signature, symbolData.kind), signature)
    }

    fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

    override fun deserializeIrSymbol(code: Long): IrSymbol {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return deserializeIrSymbolData(signature, symbolData.kind)
    }

    override fun deserializeIrType(index: Int): IrType {
        return irTypeCache.getOrPut(index) {
            val typeData = loadTypeProto(index)
            deserializeIrTypeData(typeData)
        }
    }

    override fun deserializeIdSignature(index: Int): IdSignature {
        val sigData = loadSignatureProto(index)
        return deserializeSignatureData(sigData)
    }

    override fun deserializeString(index: Int): String =
        loadStringProto(index)

    override fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoop) =
        fileLoops.getOrPut(loopIndex, loopBuilder)

    override fun deserializeExpressionBody(index: Int): org.jetbrains.kotlin.ir.expressions.IrExpression {
        return if (deserializeBodies) {
            val bodyData = loadExpressionBodyProto(index)
            deserializeExpression(bodyData)
        } else {
            val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
            IrErrorExpressionImpl(-1, -1, errorType, "Expression body is not deserialized yet")
        }
    }

    override fun deserializeStatementBody(index: Int): IrElement {
        return if (deserializeBodies) {
            val bodyData = loadStatementBodyProto(index)
            deserializeStatement(bodyData)
        } else {
            val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
            irFactory.createBlockBody(
                -1, -1, listOf(IrErrorExpressionImpl(-1, -1, errorType, "Statement body is not deserialized yet"))
            )
        }
    }

    override fun referenceIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        referenceIrSymbolData(symbol, signature)
    }

    fun deserializeFileImplicitDataIfFirstUse() {
        annotations?.let {
            file.annotations += deserializeAnnotations(it)
            annotations = null
        }
    }

    fun deserializeAllFileReachableTopLevel() {
        fileLocalDeserializationState.processPendingDeclarations()
    }
}