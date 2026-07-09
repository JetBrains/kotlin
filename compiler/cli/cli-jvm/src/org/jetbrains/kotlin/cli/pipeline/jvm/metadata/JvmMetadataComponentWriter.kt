/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
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
                val sourceFileName = getMetadataFileName(packageFragmentPart.name, packageFragmentPart.path)

                val packageFragmentFile = layout.getPackageFragmentFile(packageFqName = packageFqName, partName = sourceFileName)
                // Such duplications are not allowed in JVM compilation
                check(packageFragmentFile.exists().not()) { "Duplicate package fragment name '${packageFragmentFile.pathString}'" }
                packageFragmentFile.writeBytes(packageFragmentPart.content)

                fragmentTracker?.recordSourceFile(packageFragmentPart.path?.let { Path(it) }, packageFragmentFile)
            }
        }
    }

    // TODO(KT-87183): Switch to filename-based naming for KLIB package fragment files.
    // Until then the source file path is kept absolute and hashed into a per-file suffix that
    // disambiguate same-named files within a package. This is a temporary solution that is safe
    // for JVM incremental compilation: IC records the source-to-output mapping and, on a change,
    // deletes exactly the previously recorded output(s) and re-records the freshly produced one
    // (InputsCache.removeOutputForSourceFiles) — it never recomputes this fragment name from the
    // source path. IC stores that mapping with relative (relocatable) paths when project/build
    // dirs are supplied and absolute paths otherwise, but it round-trips per machine either way,
    // so the absolute path here is just an opaque, per-machine-deterministic token.
    private fun getMetadataFileName(firFileName: String, sourceFilePath: String?): String {
        val fileNameWithoutExtension = Path(firFileName).nameWithoutExtension
        if (sourceFilePath == null) {
            return fileNameWithoutExtension
        }

        return fileNameWithoutExtension.plus("_${sourceFilePath.cityHash64String()}")
    }

    private fun ICFileMappingTracker.recordSourceFile(sourceFile: Path?, packageFragmentFile: Path) {
        if (sourceFile != null) {
            recordSourceFilesToOutputFileMapping(listOf(sourceFile.toFile()), packageFragmentFile.toFile())
        } else {
            recordOutputFileGeneratedForPlugin(packageFragmentFile.toFile())
        }
    }
}
