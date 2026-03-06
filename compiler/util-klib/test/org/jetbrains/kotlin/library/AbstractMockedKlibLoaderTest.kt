/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KlibFile

abstract class AbstractMockedKlibLoaderTest(
    private val stdlibUniqueName: String,
    private val builtInsPlatform: BuiltInsPlatform,
) : AbstractKlibLoaderTest() {
    final override val stdlib: String by lazy {
        mockKlib(
            uniqueName = stdlibUniqueName,
            klibDir = tmpDir.resolve("stdlib"),
        ).path
    }

    final override fun compileKlib(
        asFile: Boolean,
        sourceFile: File,
        klibLocation: File,
        abiVersion: KotlinAbiVersion,
    ) {
        val klibDir = if (asFile)
            klibLocation.resolveSibling(klibLocation.name + "-dir")
        else
            tmpDir.resolve(klibLocation.name)

        mockKlib(
            uniqueName = sourceFile.nameWithoutExtension,
            klibDir = klibDir,
            abiVersion = abiVersion,
        )

        if (asFile) {
            klibDir.zipDirAs(klibLocation)
        }
    }

    private fun mockKlib(
        klibDir: File,
        uniqueName: String,
        abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
    ): File = mockKlib(klibDir) {
        manifest(
            uniqueName = uniqueName,
            builtInsPlatform = builtInsPlatform,
            versioning = KotlinLibraryVersioning(
                compilerVersion = null,
                abiVersion = abiVersion,
                metadataVersion = MetadataVersion.INSTANCE,
            )
        )
    }

    companion object {
        private fun File.zipDirAs(zipFile: File) {
            toKlibFile().zipDirAs(zipFile.toKlibFile())
        }

        private fun File.toKlibFile() = KlibFile(absolutePath)
    }
}
