/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * On the contrary to [KlibMetadataComponent], which provides read access to metadata,
 * [KlibMetadataWriterImpl] provides allows writing the metadata to the file system.
 */
internal class KlibMetadataWriterImpl(private val layout: KlibMetadataComponentLayout) {
    fun writeMetadata(serializedMetadata: SerializedMetadata) {
        layout.metadataDir.mkdirs()

        layout.moduleHeaderFile.writeBytes(serializedMetadata.module)

        serializedMetadata.fragmentNames.forEachIndexed { index, packageFqName ->
            val packageFragmentDir: KlibFile = layout.getPackageFragmentsDir(packageFqName)
            packageFragmentDir.mkdirs()

            val shortPackageName: String = packageFqName.substringAfterLast(".")
            val packageFragmentParts: List<ByteArray> = serializedMetadata.fragments[index]

            val padding: Int = packageFragmentParts.size.toString().length
            fun withPadding(packageFragmentPartIndex: Int) = String.format("%0${padding}d", packageFragmentPartIndex)

            packageFragmentParts.forEachIndexed { packageFragmentPartIndex, packageFragmentPart ->
                layout.getPackageFragmentFile(
                    packageFqName = packageFqName,
                    partName = "${withPadding(packageFragmentPartIndex)}_$shortPackageName"
                ).writeBytes(packageFragmentPart)
            }
        }
    }
}
