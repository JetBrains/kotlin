/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class KlibMetadataComponentTest {
    @TempDir
    lateinit var tmpDir: Path

    @Test
    fun `Package FQ names are listed from the metadata directory`() {
        for ((format, fileName) in listOf(KlibFormat.Directory to "unpacked", KlibFormat.ZipArchive to "packed.klib")) {
            val metadata = KlibMockDSL.generateRandomMetadata()

            val klibLocation = tmpDir.resolve(fileName)
            KlibWriter {
                format(format)
                includeMetadata(metadata)
                manifest {
                    moduleName("sample")
                    versions(KotlinLibraryVersioning(compilerVersion = null, abiVersion = null, metadataVersion = null))
                    platformAndTargets(BuiltInsPlatform.COMMON)
                }
            }.writeTo(klibLocation)

            val klib = KlibLoader { libraryPaths(klibLocation) }.load().librariesStdlibFirst.single()
            assertEquals(metadata.fragmentNames.toSet(), klib.metadata.packageFqNames, fileName)
        }
    }
}
