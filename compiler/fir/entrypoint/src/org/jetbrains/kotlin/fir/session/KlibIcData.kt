/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.utils.checkWithAttachment

class KlibIcData(incrementalData: IncrementalDataProvider) : MetadataLibrary {

    private val parts: Map<String, Map<String, ByteArray>> by lazy {
        val result = mutableMapOf<String, MutableMap<String, ByteArray>>()

        incrementalData
            .compiledPackageParts
            .toSortedMap() // This is so that IC is more deterministic
            .forEach { (file, translationResultValue) ->
                val proto = parsePackageFragment(translationResultValue.metadata)
                val fqName = proto.getExtension(KlibMetadataProtoBuf.fqName)
                val key = file.path
                val existingValue = result.getOrPut(fqName, ::mutableMapOf).put(key, translationResultValue.metadata)
                checkWithAttachment(existingValue == null, { "Duplicate metadata entry" }) {
                    it.withAttachment("key", key)
                    it.withAttachment("fqName", fqName)
                    it.withAttachment("file", file)
                }
            }

        result
    }

    val packageFragmentNameList: Collection<String>
        get() = parts.keys

    override val moduleHeaderData: ByteArray
        get() = error("moduleHeaderData is not implemented")

    override fun packageMetadataParts(fqName: String): Set<String> {
        return parts[fqName]?.keys ?: emptySet()
    }

    override fun packageMetadata(fqName: String, partName: String): ByteArray {
        return parts[fqName]?.get(partName) ?: error("Metadata not found for package $fqName part $partName")
    }
}