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

class KlibIcData(incrementalData: IncrementalDataProvider) : KlibMetadataComponent {

    private val fragments: Map<String, Map<String, ByteArray>> by lazy {
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