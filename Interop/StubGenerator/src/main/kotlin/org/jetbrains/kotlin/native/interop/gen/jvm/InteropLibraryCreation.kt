/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryWriterImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KonanLibraryVersioning
import org.jetbrains.kotlin.library.KotlinAbiVersion
import java.util.*

data class LibraryCreationArguments(
        val outputPath: String,
        val moduleName: String,
        val nativeBitcodePath: String,
        val target: KonanTarget,
        val manifest: Properties
)

fun createInteropLibrary(arguments: LibraryCreationArguments) {
    val version = KonanLibraryVersioning(
            libraryVersion = null,
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KonanVersion.CURRENT
    )
    KonanLibraryWriterImpl(
            File(arguments.outputPath),arguments.moduleName, version, arguments.target
    ).apply {
        addNativeBitcode(arguments.nativeBitcodePath)
        addManifestAddend(arguments.manifest)
        commit()
    }
}