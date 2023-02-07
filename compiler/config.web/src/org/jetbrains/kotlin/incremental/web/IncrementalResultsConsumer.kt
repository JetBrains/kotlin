/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.web

import java.io.File
import java.security.MessageDigest

interface IncrementalResultsConsumer {
    /** processes new header metadata (serialized [JsProtoBuf.Header]) */
    fun processHeader(headerMetadata: ByteArray)

    /** processes new package part metadata and binary tree for compiled source file */
    fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray)

    /**
     * [inlineFunction] is expected to be a body of inline function (an instance of [JsNode]),
     * but [Any] is used to avoid classloader conflicts in tests where the compiler is isolated
     * (such as [JsProtoComparisonTestGenerated]).
     */
    fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any, line: Int, column: Int)

    /**
     * Alternative to [processInlineFunction]: record all inline functions after it was processed.
     * Used in daemon RPC.
     */
    fun processInlineFunctions(functions: Collection<JsInlineFunctionHash>)

    fun processPackageMetadata(packageName: String, metadata: ByteArray)

    fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?
    )
}

interface IncrementalNextRoundChecker {
    fun checkProtoChanges(sourceFile: File, packagePartMetadata: ByteArray)
    fun shouldGoToNextRound(): Boolean
}

class FunctionWithSourceInfo(val expression: Any, val line: Int, val column: Int) {
    val md5: Long
        get() = "($line:$column)$expression".toByteArray().md5()
}

open class IncrementalResultsConsumerImpl : IncrementalResultsConsumer {
    lateinit var headerMetadata: ByteArray
        private set

    private val _packageParts = hashMapOf<File, TranslationResultValue>()
    val packageParts: Map<File, TranslationResultValue>
        get() = _packageParts

    private val _deferInlineFuncs = hashMapOf<File, MutableMap<String, FunctionWithSourceInfo>>()
    private var _processedInlineFuncs: Collection<JsInlineFunctionHash>? = null
    val inlineFunctions: Map<File, Map<String, Long>>
        get() {
            val result = HashMap<File, MutableMap<String, Long>>(_deferInlineFuncs.size)

            for ((file, inlineFnsFromFile) in _deferInlineFuncs) {
                val functionsHashes = HashMap<String, Long>(inlineFnsFromFile.size)

                for ((fqName, fn) in inlineFnsFromFile) {
                    functionsHashes[fqName] = fn.md5
                }

                result[file] = functionsHashes
            }

            _processedInlineFuncs?.forEach {
                val fileFunctions = result.getOrPut(File(it.sourceFilePath)) { mutableMapOf() }
                fileFunctions[it.fqName] = it.inlineFunctionMd5Hash
            }

            return result
        }

    override fun processHeader(headerMetadata: ByteArray) {
        this.headerMetadata = headerMetadata
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray) {
        _packageParts.put(sourceFile, TranslationResultValue(packagePartMetadata, binaryAst, inlineData))
    }

    override fun processInlineFunctions(functions: Collection<JsInlineFunctionHash>) {
        check(_processedInlineFuncs == null)
        _processedInlineFuncs = functions
    }

    override fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any, line: Int, column: Int) {
        val mapForSource = _deferInlineFuncs.getOrPut(sourceFile) { hashMapOf() }
        mapForSource[fqName] = FunctionWithSourceInfo(inlineFunction, line, column)
    }

    private val _packageMetadata = hashMapOf<String, ByteArray>()
    val packageMetadata: Map<String, ByteArray>
        get() = _packageMetadata

    override fun processPackageMetadata(packageName: String, metadata: ByteArray) {
        _packageMetadata[packageName] = metadata
    }

//    class IrFileData(fileData: ByteArray, symbols: ByteArray, types: ByteArray, strings: ByteArray, bodies: ByteArray, declarations: ByteArray)
    private val _irFileData = hashMapOf<File, IrTranslationResultValue>()
    val irFileData: Map<File, IrTranslationResultValue>
        get() = _irFileData

    override fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?
    ) {
        _irFileData[sourceFile] = IrTranslationResultValue(fileData, types, signatures, strings, declarations, bodies, fqn, debugInfo)
    }
}

private fun ByteArray.md5(): Long {
    val d = MessageDigest.getInstance("MD5").digest(this)!!
    return ((d[0].toLong() and 0xFFL)
            or ((d[1].toLong() and 0xFFL) shl 8)
            or ((d[2].toLong() and 0xFFL) shl 16)
            or ((d[3].toLong() and 0xFFL) shl 24)
            or ((d[4].toLong() and 0xFFL) shl 32)
            or ((d[5].toLong() and 0xFFL) shl 40)
            or ((d[6].toLong() and 0xFFL) shl 48)
            or ((d[7].toLong() and 0xFFL) shl 56))
}

