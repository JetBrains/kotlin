/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import java.io.File

abstract class AbstractMockedKlibLoaderTest(
    private val stdlibUniqueName: String,
    private val builtInsPlatform: BuiltInsPlatform,
) : AbstractKlibLoaderTest() {
    final override val stdlib: String by lazy {
        mockKlib(
            uniqueName = stdlibUniqueName,
            klibDir = tmpDir.resolve("stdlib"),
            withCompanionBlocksAndExtensionsFeature = false,
        ).path
    }

    final override fun compileKlib(
        parameters: CompilationParameters
    ) {
        val klibDir = if (parameters.asFile)
            parameters.klibLocation.resolveSibling(parameters.klibLocation.name + "-dir")
        else
            tmpDir.resolve(parameters.klibLocation.name)

        mockKlib(
            uniqueName = parameters.sourceFile.nameWithoutExtension,
            klibDir = klibDir,
            abiVersion = parameters.abiVersion,
            withCompanionBlocksAndExtensionsFeature = parameters.withCompanionBlocksAndExtensionsFeature,
        )

        if (parameters.asFile) {
            klibDir.zipDirAs(parameters.klibLocation)
        }
    }

    private fun mockKlib(
        klibDir: File,
        uniqueName: String,
        abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
        withCompanionBlocksAndExtensionsFeature: Boolean,
    ): File = mockKlib(klibDir) {
        manifest(
            uniqueName = uniqueName,
            builtInsPlatform = builtInsPlatform,
            versioning = KotlinLibraryVersioning(
                compilerVersion = null,
                abiVersion = abiVersion,
                metadataVersion = MetadataVersion.INSTANCE,
            )
        ) {
            // This is only needed to simulate that the mock KLIB has some ABI.
            this[KLIB_PROPERTY_IR_PROVIDER] = "simulation_of_some_ir_provider"
            this[KLIB_PROPERTY_NEW_COMPANION_INITIALIZATION] = withCompanionBlocksAndExtensionsFeature.toString()
        }
    }

    companion object {
        private fun File.zipDirAs(zipFile: File) {
            toPath().zipDirAs(zipFile.toPath())
        }
    }
}
