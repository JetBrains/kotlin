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

typealias Hash = Long // Any long hash
typealias FlatHash = Hash // Hash of inline function without its underlying inline call tree
typealias TransHash = Hash // Hash of inline function including its underlying inline call tree

private val md5 = MessageDigest.getInstance("MD5")

private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

private fun createFileCacheId(fileName: String): String = md5.digest(fileName.encodeToByteArray()).toHex()

interface PersistentCacheProvider {
    fun fileFingerPrint(path: String): Hash

    fun serializedParts(path: String): SerializedIcDataForFile

    fun inlineGraphForFile(path: String, sigResolver: (Int) -> IdSignature): Collection<Pair<IdSignature, TransHash>>

    fun inlineHashes(path: String, sigResolver: (Int) -> IdSignature): Map<IdSignature, TransHash>

    fun allInlineHashes(sigResolver: (String, Int) -> IdSignature): Map<IdSignature, TransHash>
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

    override fun serializedParts(path: String): SerializedIcDataForFile {
        val fileDir = path.fileDir
        return fileDir.readIcDataBinary()
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
        cachePath.listFiles { file: File -> file.isDirectory }!!.forEach {
            val fileInfo = File(it, fileInfoFile)
            if (fileInfo.exists()) {
                val fileName = fileInfo.readLines()[0]
                parseHashList(it, inlineFunctionsFile) { id -> sigResolver(fileName, id) }.forEach {
                    result[it.first] = it.second
                }
            }
        }
        return result
    }

}

interface PersistentCacheConsumer {
    fun commitInlineFunctions(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int)
    fun commitFileFingerPrint(path: String, fingerprint: Hash)
    fun commitInlineGraph(path: String, hashes: Collection<Pair<IdSignature, TransHash>>, sigResolver: (IdSignature) -> Int)
    fun commitICCacheData(path: String, icData: SerializedIcDataForFile)
    fun invalidateForFile(path: String)
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

    override fun commitICCacheData(path: String, icData: SerializedIcDataForFile) {
        val fileId = createFileCacheId(path)
        val fileDir = File(File(cachePath), fileId)
        icData.writeData(fileDir)
    }

    override fun invalidateForFile(path: String) {
        val fileId = createFileCacheId(path)
        val cacheDir = File(cachePath)
        val fileDir = File(cacheDir, fileId)
        File(fileDir, inlineFunctionsFile).delete()
        File(fileDir, inlineGraphFile).delete()
        File(fileDir, fileInfoFile).delete()
        // TODO: once per-file invalidation is integrated into IC delete the whole directory including PIR parts
        //fileDir.deleteRecursively()
    }
}
