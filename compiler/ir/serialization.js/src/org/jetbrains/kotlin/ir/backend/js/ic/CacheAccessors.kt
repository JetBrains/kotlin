/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.util.IdSignature
import java.io.File
import java.io.PrintWriter
import java.security.MessageDigest


private const val inlineGraphFile = "inline.graph"
private const val inlineFunctionsFile = "inline.functions"
private const val fileInfoFile = "file.info"
private const val fileBinaryAst = "binary.ast"
private const val fileBinaryDts = "binary.dst"
private const val fileSourceMap = "source.map"

typealias Hash = Long // Any long hash
typealias FlatHash = Hash // Hash of inline function without its underlying inline call tree
typealias TransHash = Hash // Hash of inline function including its underlying inline call tree

private val md5 = MessageDigest.getInstance("MD5")

private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

private fun createFileCacheId(fileName: String): String = md5.digest(fileName.encodeToByteArray()).toHex()

class ICCache()

class FileCache(val name: String, var ast: ByteArray?, var dts: ByteArray?, var sourceMap: ByteArray?)
class ModuleCache(val name: String, val asts: Map<String, FileCache>)

interface PersistentCacheProvider {
    fun fileFingerPrint(path: String): Hash

    fun inlineGraphForFile(path: String, sigResolver: (Int) -> IdSignature): Collection<Pair<IdSignature, TransHash>>

    fun inlineHashes(path: String, sigResolver: (Int) -> IdSignature): Map<IdSignature, TransHash>

    fun allInlineHashes(sigResolver: (String, Int) -> IdSignature): Map<IdSignature, TransHash>

    fun binaryAst(path: String): ByteArray?

    fun dts(path: String): ByteArray?

    fun sourceMap(path: String): ByteArray?

    fun filePaths(): Iterable<String>

    fun moduleName(): String

    companion object {
        val EMPTY = object : PersistentCacheProvider {
            override fun fileFingerPrint(path: String): Hash {
                return 0
            }

            override fun inlineGraphForFile(path: String, sigResolver: (Int) -> IdSignature): Collection<Pair<IdSignature, TransHash>> {
                return emptyList()
            }

            override fun inlineHashes(path: String, sigResolver: (Int) -> IdSignature): Map<IdSignature, TransHash> {
                return emptyMap()
            }

            override fun allInlineHashes(sigResolver: (String, Int) -> IdSignature): Map<IdSignature, TransHash> {
                return emptyMap()
            }

            override fun binaryAst(path: String): ByteArray {
                return ByteArray(0)
            }

            override fun dts(path: String): ByteArray? {
                return null
            }

            override fun sourceMap(path: String): ByteArray? {
                return null
            }

            override fun filePaths(): Iterable<String> = emptyList()

            override fun moduleName(): String {
                TODO("Not yet implemented")
            }
        }
    }
}


class PersistentCacheProviderImpl(private val cachePath: String) : PersistentCacheProvider {

    private val String.fileDir: File
        get() {
            val fileId = createFileCacheId(this)
            return File(File(cachePath), fileId)
        }

    private fun String.parseHash(): Hash = java.lang.Long.parseUnsignedLong(this, 16)

    override fun fileFingerPrint(path: String): Hash {
        val dataFile = File(path.fileDir, fileInfoFile)
        return if (dataFile.exists()) {
            val hashLine = dataFile.readLines()[1]
            hashLine.parseHash()
        } else 0
    }

    private fun parseHashList(
        fileDir: File,
        fileName: String,
        sigResolver: (Int) -> IdSignature
    ): Collection<Pair<IdSignature, TransHash>> {
        val inlineGraphFile = File(fileDir, fileName)

        if (inlineGraphFile.exists()) {

            return inlineGraphFile.readLines().mapNotNull { line ->
                val (sigIdS, hashString) = line.split(":")
                val sigId = Integer.parseInt(sigIdS)
                try {
                    val idSig = sigResolver(sigId)
                    val tHash = hashString.parseHash()
                    idSig to tHash
                } catch (ex: IndexOutOfBoundsException) {
                    // println("Signature [$sigId] has been removed")
                    null
                }
            }
        }

        return emptyList()
    }

    override fun inlineGraphForFile(path: String, sigResolver: (Int) -> IdSignature): Collection<Pair<IdSignature, TransHash>> {
        return parseHashList(path.fileDir, inlineGraphFile, sigResolver)
    }

    override fun inlineHashes(path: String, sigResolver: (Int) -> IdSignature): Map<IdSignature, TransHash> {
        return parseHashList(path.fileDir, inlineFunctionsFile, sigResolver).toMap()
    }

    override fun allInlineHashes(sigResolver: (String, Int) -> IdSignature): Map<IdSignature, TransHash> {
        val result = mutableMapOf<IdSignature, TransHash>()
        val cachePath = File(cachePath)
        cachePath.listFiles { file: File -> file.isDirectory }!!.forEach { f ->
            val fileInfo = File(f, fileInfoFile)
            if (fileInfo.exists()) {
                val fileName = fileInfo.readLines()[0]
                parseHashList(f, inlineFunctionsFile) { id -> sigResolver(fileName, id) }.forEach { (sig, hash) ->
                    result[sig] = hash
                }
            }
        }
        return result
    }

    private fun readBytesFromCacheFile(fileName: String, cacheName: String): ByteArray? {
        val cachePath = fileName.fileDir
        val cacheFile = File(cachePath, cacheName)
        if (cacheFile.exists()) return cacheFile.readBytes()
        return null
    }

    override fun binaryAst(path: String): ByteArray? {
        return readBytesFromCacheFile(path, fileBinaryAst)
    }

    override fun dts(path: String): ByteArray? {
        return readBytesFromCacheFile(path, fileBinaryDts)
    }

    override fun sourceMap(path: String): ByteArray? {
        return readBytesFromCacheFile(path, fileSourceMap)
    }

    override fun filePaths(): Iterable<String> {
        val files = File(cachePath).listFiles() ?: return emptyList()
        return files.filter { it.isDirectory }.mapNotNull { f ->
            val fileInfo = File(f, fileInfoFile)
            if (fileInfo.exists()) {
                fileInfo.readLines()[0]
            } else null
        }
    }

    override fun moduleName(): String {
        val infoFile = File(File(cachePath), "info")
        return infoFile.readLines()[3]
    }
}

interface PersistentCacheConsumer {
    fun commitInlineFunctions(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int)
    fun commitFileFingerPrint(path: String, fingerprint: Hash)
    fun commitInlineGraph(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int)
    fun commitBinaryAst(path: String, astData: ByteArray)
    fun commitBinaryDts(path: String, dstData: ByteArray)
    fun commitSourceMap(path: String, mapData: ByteArray)
    fun invalidateForFile(path: String)

    fun commitLibraryInfo(libraryPath: String, flatHash: ULong, transHash: ULong, configHash: ULong, moduleName: String)

    companion object {
        val EMPTY = object : PersistentCacheConsumer {
            override fun commitInlineFunctions(
                path: String,
                hashes: Collection<Pair<IdSignature, TransHash>>,
                sigResolver: (IdSignature) -> Int
            ) {
            }

            override fun commitFileFingerPrint(path: String, fingerprint: Hash) {
            }

            override fun commitInlineGraph(
                path: String,
                hashes: Collection<Pair<IdSignature, TransHash>>,
                sigResolver: (IdSignature) -> Int
            ) {
            }

            override fun invalidateForFile(path: String) {
            }

            override fun commitBinaryAst(path: String, astData: ByteArray) {

            }

            override fun commitLibraryInfo(libraryPath: String, flatHash: ULong, transHash: ULong, configHash: ULong, moduleName: String) {

            }

            override fun commitBinaryDts(path: String, dstData: ByteArray) {

            }

            override fun commitSourceMap(path: String, mapData: ByteArray) {

            }
        }
    }
}


class PersistentCacheConsumerImpl(private val cachePath: String) : PersistentCacheConsumer {

    private fun commitFileHashMapping(
        path: String,
        cacheDst: String,
        hashes: Collection<Pair<IdSignature, TransHash>>,
        sigResolver: (IdSignature) -> Int
    ) {
        val fileId = createFileCacheId(path)
        val fileDir = File(File(cachePath), fileId)
        val destination = File(fileDir, cacheDst)
        fileDir.mkdirs()
        destination.createNewFile()
        PrintWriter(destination).use {
            for (hashData in hashes) {
                val (sig, hash) = hashData
                val sigId = sigResolver(sig)
                val hashString = hash.toULong().toString(16)
                it.println("$sigId:$hashString")
            }
        }
    }

    override fun commitInlineFunctions(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int) {
        commitFileHashMapping(path, inlineFunctionsFile, hashes, sigResolver)
    }

    override fun commitFileFingerPrint(path: String, fingerprint: Hash) {
        val fileId = createFileCacheId(path)
        val fileDir = File(File(cachePath), fileId)
        fileDir.mkdirs()
        val infoFile = File(fileDir, fileInfoFile)
        if (infoFile.exists()) infoFile.delete()
        infoFile.createNewFile()
        PrintWriter(infoFile).use {
            it.println(path)
            it.println(fingerprint.toULong().toString(16))
        }
    }

    override fun commitInlineGraph(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int) {
        commitFileHashMapping(path, inlineGraphFile, hashes, sigResolver)
    }

    override fun invalidateForFile(path: String) {
        val fileId = createFileCacheId(path)
        val cacheDir = File(cachePath)
        val fileDir = File(cacheDir, fileId)
        File(fileDir, inlineFunctionsFile).delete()
        File(fileDir, inlineGraphFile).delete()
        File(fileDir, fileInfoFile).delete()
        File(fileDir, fileBinaryAst).delete()
        // TODO: once per-file invalidation is integrated into IC delete the whole directory including PIR parts
        //fileDir.deleteRecursively()
    }

    private fun commitByteArrayToCacheFile(fileName: String, cacheName: String, data: ByteArray) {
        val fileId = createFileCacheId(fileName)
        val cacheDir = File(File(cachePath), fileId)
        val cacheFile = File(cacheDir, cacheName)
        if (cacheFile.exists()) cacheFile.delete()
        cacheFile.createNewFile()
        cacheFile.writeBytes(data)
    }

    override fun commitBinaryAst(path: String, astData: ByteArray) {
        commitByteArrayToCacheFile(path, fileBinaryAst, astData)
    }

    override fun commitBinaryDts(path: String, dstData: ByteArray) {
        commitByteArrayToCacheFile(path, fileBinaryDts, dstData)
    }

    override fun commitSourceMap(path: String, mapData: ByteArray) {
        commitByteArrayToCacheFile(path, fileSourceMap, mapData)
    }

    override fun commitLibraryInfo(libraryPath: String, flatHash: ULong, transHash: ULong, configHash: ULong, moduleName: String) {
        val infoFile = File(File(cachePath), "info")
        if (infoFile.exists()) {
            infoFile.delete()
        }
        infoFile.createNewFile()

        PrintWriter(infoFile).use {
            it.println(libraryPath)
            it.println(flatHash.toString(16))
            it.println(transHash.toString(16))
            it.println(configHash.toString(16))
            it.println(moduleName)
        }
    }
}
