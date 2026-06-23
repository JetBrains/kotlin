/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.library.SerializedFirMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import java.nio.file.Path
import kotlin.io.path.*

/**
 * An implementation of [KlibComponentWriter] that writes [SerializedFirMetadata] to the constructed Klib library.
 */
internal class JvmMetadataComponentWriter(
    private val metadata: SerializedFirMetadata,
    private val fragmentTracker: ICFileMappingTracker?,
) : KlibComponentWriter {
    override fun writeTo(root: Path) {
        val layout = KlibMetadataComponentLayout(root)

        layout.metadataDir.createDirectories()
        layout.moduleHeaderFile.writeBytes(metadata.module)

        metadata.fragmentNames.forEachIndexed { index, packageFqName ->
            layout.getPackageFragmentsDir(packageFqName).createDirectories()

            metadata.fragments[index].forEach { packageFragmentPart ->
                val sourceFile = packageFragmentPart.path?.let { Path(it) }
                val sourceFileName = packageFragmentPart.name

                val packageFragmentFile = layout.getPackageFragmentFile(packageFqName = packageFqName, partName = sourceFileName)
                // Such duplications are not allowed in JVM compilation
                check(packageFragmentFile.exists().not()) { "Duplicate package fragment name '${packageFragmentFile.pathString}'" }
                packageFragmentFile.writeBytes(packageFragmentPart.content)

                fragmentTracker?.recordSourceFile(sourceFile, packageFragmentFile)
            }
        }
    }

    private fun ICFileMappingTracker.recordSourceFile(sourceFile: Path?, packageFragmentFile: Path) {
        if (sourceFile != null) {
            recordSourceFilesToOutputFileMapping(listOf(sourceFile.toFile()), packageFragmentFile.toFile())
        } else {
            recordOutputFileGeneratedForPlugin(packageFragmentFile.toFile())
        }
    }
}
