/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedFragment
import org.jetbrains.kotlin.library.SerializedFragmentWithSource
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.library.writer.KlibWrittenMetadataPackageFragmentTracker
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import org.jetbrains.kotlin.konan.file.File as KlibFile

/**
 * An implementation of [KlibComponentWriter] that writes [SerializedMetadata] to the constructed Klib library.
 */
internal class KlibMetadataComponentWriterImpl(
    private val metadata: SerializedMetadata,
    private val fragmentTracker: KlibWrittenMetadataPackageFragmentTracker?,
) : KlibComponentWriter {
    override fun writeTo(root: KlibFile) {
        val layout = KlibMetadataComponentLayout(root)
        layout.metadataDir.mkdirs()

        layout.moduleHeaderFile.writeBytes(metadata.module)

        metadata.fragmentNames.forEachIndexed { index, packageFqName ->
            val packageFragmentDir: KlibFile = layout.getPackageFragmentsDir(packageFqName)
            packageFragmentDir.mkdirs()

            val shortPackageName: String = packageFqName.substringAfterLast(".")
            val (packageFragmentWithSourceParts, packageFragmentWithoutSourceParts) =
                metadata.fragments[index].partition { it.sourceFilePathOrNull != null }

            val padding: Int = packageFragmentWithoutSourceParts.size.toString().length
            packageFragmentWithoutSourceParts.forEachIndexed { partIndex, packageFragmentPart ->
                val partName = "${partIndex.toString().padStart(padding, '0')}_$shortPackageName"
                layout.writeFragment(packageFqName, partName, packageFragmentPart.content, sourceFile = null)
            }

            packageFragmentWithSourceParts.forEach { packageFragmentPart ->
                val sourceFile = Path(requireNotNull(packageFragmentPart.sourceFilePathOrNull))
                val sourceFileName = sourceFile.nameWithoutExtension

                layout.writeFragment(packageFqName, sourceFileName, packageFragmentPart.content, sourceFile)
            }
        }
    }

    private val SerializedFragment.sourceFilePathOrNull: String?
        get() = (this as? SerializedFragmentWithSource)?.sourceFilePath

    private fun KlibMetadataComponentLayout.writeFragment(
        packageFqName: String,
        partName: String,
        content: ByteArray,
        sourceFile: Path?,
    ) {
        val packageFragmentFile = getPackageFragmentFile(packageFqName = packageFqName, partName = partName)
        check(packageFragmentFile.exists.not()) { "Duplicate package fragment name '${packageFragmentFile.path}'" }
        packageFragmentFile.writeBytes(content)

        fragmentTracker?.recordSourceFile(sourceFile, packageFragmentFile.javaPath())
    }
}
