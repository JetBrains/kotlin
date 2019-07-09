/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.MetadataKotlinLibraryLayout
import org.jetbrains.kotlin.library.MetadataWriter
import org.jetbrains.kotlin.library.SerializedMetadata

class MetadataWriterImpl(val metadataLayout: MetadataKotlinLibraryLayout) : MetadataWriter {
    init {
        metadataLayout.metadataDir.mkdirs()
    }

    override fun addMetadata(metadata: SerializedMetadata) {
        metadataLayout.moduleHeaderFile.writeBytes(metadata.module)
        metadata.fragments.forEachIndexed { index, it ->
            val packageFqName = metadata.fragmentNames[index]
            val shortName = packageFqName.substringAfterLast(".")
            val dir = metadataLayout.packageFragmentsDir(packageFqName)
            dir.deleteRecursively()
            dir.mkdirs()
            val numCount = it.size.toString().length
            fun withLeadingZeros(i: Int) = String.format("%0${numCount}d", i)
            for ((i, fragment) in it.withIndex()) {
                metadataLayout.packageFragmentFile(packageFqName, "${withLeadingZeros(i)}_$shortName")
                    .writeBytes(fragment)
            }
        }
    }
}
