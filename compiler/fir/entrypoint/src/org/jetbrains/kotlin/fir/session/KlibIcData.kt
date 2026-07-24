/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.utils.checkWithAttachment
import java.io.File

class KlibIcData(nonDirtyPreviousPackageFragments: Map<File, ByteArray>) : KlibMetadataComponent {

    constructor(incrementalData: IncrementalDataProvider) : this(incrementalData.compiledPackageParts.mapValues { [file, result] -> result.metadata })

    private val fragments: Map<String, Map<String, ByteArray>> by lazy {
        val result = mutableMapOf<String, MutableMap<String, ByteArray>>()

        nonDirtyPreviousPackageFragments
            .toSortedMap() // This is so that IC is more deterministic
            .forEach { [file, metadata] ->
                val proto = parsePackageFragment(metadata)
                val fqName = proto.getExtension(KlibMetadataProtoBuf.fqName)
                val key = file.path
                val existingValue = result.getOrPut(fqName, ::mutableMapOf).put(key, metadata)
                checkWithAttachment(existingValue == null, { "Duplicate metadata entry" }) {
                    it.withAttachment("key", key)
                    it.withAttachment("fqName", fqName)
                    it.withAttachment("file", file)
                }
            }

        result
    }

    val packageFragmentNameList: Collection<String>
        get() = fragments.keys

    override val moduleHeaderData: Nothing
        get() = error("moduleHeaderData is not implemented")

    override fun getPackageFragmentNames(packageFqName: String): Set<String> {
        return fragments[packageFqName]?.keys ?: emptySet()
    }

    override fun getPackageFragment(packageFqName: String, fragmentName: String): ByteArray {
        return fragments[packageFqName]?.get(fragmentName) ?: error("Metadata not found for package $packageFqName part $fragmentName")
    }
}
