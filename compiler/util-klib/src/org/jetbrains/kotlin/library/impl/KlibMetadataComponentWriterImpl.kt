/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * An implementation of [KlibComponentWriter] that writes [SerializedMetadata] to the constructed Klib library.
 */
internal class KlibMetadataComponentWriterImpl(
    private val metadata: SerializedMetadata
) : KlibComponentWriter {
    override fun writeTo(root: KlibFile) {
        val layout = KlibMetadataComponentLayout(root)
        layout.metadataDir.mkdirs()

        layout.moduleHeaderFile.writeBytes(metadata.module)

        metadata.fragmentNames.forEachIndexed { index, packageFqName ->
            val packageFragmentDir: KlibFile = layout.getPackageFragmentsDir(packageFqName)
            packageFragmentDir.mkdirs()

            val shortPackageName: String = packageFqName.substringAfterLast(".")
            val packageFragmentParts: List<ByteArray> = metadata.fragments[index]

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
