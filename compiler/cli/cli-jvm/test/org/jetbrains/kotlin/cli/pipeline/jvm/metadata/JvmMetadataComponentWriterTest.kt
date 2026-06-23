/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.library.SerializedFirFile
import org.jetbrains.kotlin.library.SerializedFirMetadata
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class JvmMetadataComponentWriterTest {
    private class RecordingFileMappingTracker : ICFileMappingTracker {
        val recordedMappings = mutableListOf<Pair<File?, File>>()

        override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {
            recordedMappings += sourceFiles.single() to outputFile
        }

        override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) {}

        override fun recordOutputFileGeneratedForPlugin(outputFile: File) {
            recordedMappings += null to outputFile
        }
    }

    @Test
    @DisplayName("Each written fragment is reported to the file-mapping tracker")
    fun reportsSourceToOutputMappingForEachFragment(@TempDir klibDir: File) {
        val tracker = RecordingFileMappingTracker()
        val content = ByteArray(10)
        val root = klibDir.toPath()

        JvmMetadataComponentWriter(
            SerializedFirMetadata(
                module = content,
                fragments = listOf(
                    listOf(
                        SerializedFirFile(name = "a", content = content, path = "/src/a.kt"),
                        SerializedFirFile(name = "0_", content = content, path = null),
                    ),
                    listOf(
                        SerializedFirFile(name = "0_bar", content = content, path = null),
                    ),
                ),
                fragmentNames = listOf("", "foo.bar"),
                metadataVersion = MetadataVersion.INSTANCE.toArray(),
            ),
            tracker,
        ).writeTo(root)

        val layout = KlibMetadataComponentLayout(root)
        val expectedMappings = listOf(
            File("/src/a.kt") to layout.getPackageFragmentFile(packageFqName = "", partName = "a").toFile(),
            null to layout.getPackageFragmentFile(packageFqName = "", partName = "0_").toFile(),
            null to layout.getPackageFragmentFile(packageFqName = "foo.bar", partName = "0_bar").toFile(),
        )
        assertEquals(expectedMappings, tracker.recordedMappings)
    }

    @Test
    @DisplayName("Writing two fragments with the same name in one package fails with a duplicate-fragment error")
    fun failsOnDuplicateFragmentNamesWithinPackage(@TempDir klibDir: File) {
        val content = ByteArray(10)
        val root = klibDir.toPath()

        assertThrows<IllegalStateException> {
            JvmMetadataComponentWriter(
                SerializedFirMetadata(
                    module = content,
                    fragments = listOf(
                        listOf(
                            SerializedFirFile(name = "a", content = content, path = "/src/x/a.kt"),
                            SerializedFirFile(name = "a", content = content, path = "/src/y/a.kt"),
                        ),
                    ),
                    fragmentNames = listOf("foo.bar"),
                    metadataVersion = MetadataVersion.INSTANCE.toArray(),
                ),
                ICFileMappingTracker.DoNothing,
            ).writeTo(root)
        }
    }
}
