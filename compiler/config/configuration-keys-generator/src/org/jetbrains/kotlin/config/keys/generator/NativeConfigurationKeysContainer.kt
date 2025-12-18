/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

@Suppress("unused")
object NativeConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.konan.config", "NativeConfigurationKeys") {
    val KONAN_HOME by key<String>("overridden compiler distribution path", throwOnNull = false)

    val KONAN_LIBRARIES by key<List<String>>("library file paths")
    val KONAN_FRIEND_LIBRARIES by key<List<String>>("friend library paths")
    val KONAN_REFINES_MODULES by key<List<String>>("refines module paths")
    val KONAN_INCLUDED_LIBRARIES by key<List<String>>("klibs processed in the same manner as source files")

    val KONAN_MANIFEST_ADDEND by key<String>("provide manifest addend file", throwOnNull = false)
    val KONAN_GENERATED_HEADER_KLIB_PATH by key<String>("path to file where header klib should be produced", throwOnNull = false)

    val KONAN_NATIVE_LIBRARIES by key<List<String>>("native library file paths")
    val KONAN_INCLUDED_BINARIES by key<List<String>>("included binary file paths")

    val KONAN_PRODUCED_ARTIFACT_KIND by key<CompilerOutputKind>("compiler output kind")

    val KONAN_NO_STDLIB by key<Boolean>("don't link with stdlib")
    val KONAN_NO_DEFAULT_LIBS by key<Boolean>("don't link with the default libraries")
    val KONAN_PURGE_USER_LIBS by key<Boolean>("purge user-specified libs too")

    val KONAN_DONT_COMPRESS_KLIBS by key<Boolean>("don't the library into a klib file")

    val KONAN_OUTPUT_PATH by key<String>("program or library name", throwOnNull = false)

    val KONAN_SHORT_MODULE_NAME by key<String>("short module name for IDE and export")
    val KONAN_TARGET by key<String>("target we compile for", throwOnNull = false)

    val KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO by key<String>("write dependencies of the klib being produced to the given path", throwOnNull = false)
    val KONAN_MANIFEST_NATIVE_TARGETS by key<List<KonanTarget>>("value of native_targets property to write in manifest")
}
