/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.common.serialization.cityHash128
import org.jetbrains.kotlin.backend.common.serialization.cityHash128WithSeed
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import java.io.File

@JvmInline
value class FingerprintHash(val hash: Hash128Bits) {
    override fun toString(): String {
        return "${hash.lowBytes.toString(Character.MAX_RADIX)}$HASH_SEPARATOR${hash.highBytes.toString(Character.MAX_RADIX)}"
    }

    companion object {
        private const val HASH_SEPARATOR = "."

        internal fun fromString(s: String): FingerprintHash? {
            val hashes = s.split(HASH_SEPARATOR).mapNotNull { it.toULongOrNull(Character.MAX_RADIX) }
            return hashes.takeIf { it.size == 2 }?.let { FingerprintHash(Hash128Bits(lowBytes = it[0], highBytes = it[1])) }
        }
    }
}

@JvmInline
value class SerializedIrFileFingerprint private constructor(val fileFingerprint: FingerprintHash) {
    companion object {
        internal fun fromString(s: String): SerializedIrFileFingerprint? {
            return FingerprintHash.fromString(s)?.let { SerializedIrFileFingerprint(it) }
        }

        private fun calculateFileFingerprint(file: SerializedIrFile): FingerprintHash {
            val fileDataHash = cityHash128(file.fileData)
            val withTypesHash = cityHash128WithSeed(fileDataHash, file.types)
            val withSignaturesHash = cityHash128WithSeed(withTypesHash, file.signatures)
            val withStringsHash = cityHash128WithSeed(withSignaturesHash, file.strings)
            val withBodiesHash = cityHash128WithSeed(withStringsHash, file.bodies)
            return FingerprintHash(cityHash128WithSeed(withBodiesHash, file.declarations))
        }

        private fun calculateFileFingerprint(lib: KotlinLibrary, fileIndex: Int): FingerprintHash {
            val fileDataHash = cityHash128(lib.file(fileIndex))
            val withTypesHash = cityHash128WithSeed(fileDataHash, lib.types(fileIndex))
            val withSignaturesHash = cityHash128WithSeed(withTypesHash, lib.signatures(fileIndex))
            val withStringsHash = cityHash128WithSeed(withSignaturesHash, lib.strings(fileIndex))
            val withBodiesHash = cityHash128WithSeed(withStringsHash, lib.bodies(fileIndex))
            return FingerprintHash(cityHash128WithSeed(withBodiesHash, lib.declarations(fileIndex)))
        }
    }

    constructor(file: SerializedIrFile) : this(calculateFileFingerprint(file))

    constructor(lib: KotlinLibrary, fileIndex: Int) : this(calculateFileFingerprint(lib, fileIndex))
}

@JvmInline
value class SerializedKlibFingerprint(val klibFingerprint: FingerprintHash) {
    companion object {
        private fun List<SerializedIrFileFingerprint>.calculateKlibFingerprint(): FingerprintHash {
            val combinedHash = fold(Hash128Bits(size.toULong())) { acc, x ->
                acc.combineWith(x.fileFingerprint.hash)
            }
            return FingerprintHash(combinedHash)
        }

        // File.calculateKlibHash() and List<SerializedIrFileFingerprint>.calculateKlibFingerprint()
        // give different hashes for same klibs, it is ok
        private fun File.calculateKlibHash(prefix: String = "", seed: Hash128Bits = Hash128Bits()): Hash128Bits {
            var combinedHash = seed

            if (isDirectory) {
                listFiles()!!.sortedBy { it.name }.forEach { f ->
                    val filePrefix = "$prefix${f.name}/"
                    combinedHash = f.calculateKlibHash(filePrefix, cityHash128WithSeed(combinedHash, filePrefix.toByteArray()))
                }
            } else {
                forEachBlock { buffer, bufferSize ->
                    combinedHash = cityHash128WithSeed(combinedHash, buffer, 0, bufferSize)
                }
            }

            return combinedHash
        }

        internal fun fromString(s: String): SerializedKlibFingerprint? {
            return FingerprintHash.fromString(s)?.let { SerializedKlibFingerprint(it) }
        }
    }

    constructor(fileFingerprints: List<SerializedIrFileFingerprint>) : this(fileFingerprints.calculateKlibFingerprint())

    constructor(klibFile: File) : this(FingerprintHash(klibFile.calculateKlibHash()))
}

private const val FILE_FINGERPRINTS_SEPARATOR = " "

internal fun List<SerializedIrFileFingerprint>.joinIrFileFingerprints(): String {
    return joinToString(FILE_FINGERPRINTS_SEPARATOR)
}

internal fun String.parseSerializedIrFileFingerprints(): List<SerializedIrFileFingerprint> {
    return split(FILE_FINGERPRINTS_SEPARATOR).mapNotNull(SerializedIrFileFingerprint::fromString)
}
