/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.io.File
import java.nio.file.Paths

object KotlinNativePaths {
    private val validPropertiesNames = listOf(
        "kotlin.native.home", "org.jetbrains.kotlin.native.home", "konan.home"
    )
    private val kotlinNativeHome
        get() = validPropertiesNames.firstNotNullOfOrNull(System::getProperty)

    private fun defaultHomePath(): File {
        val jarPath = PathUtil.getResourcePathForClass(this::class.java)

        // Check that the path obtained really points to the distribution.
        check(
            jarPath.toPath().endsWith(Paths.get("konan/lib/kotlin-native.jar")) || jarPath.toPath()
                .endsWith(Paths.get("konan/lib/kotlin-native-compiler-embeddable.jar"))
        ) {
            val classesPath = if (jarPath.extension == "jar") jarPath else jarPath.parentFile
            """
                    Cannot determine a compiler distribution directory.
                    A path to compiler classes is not a part of a distribution: ${classesPath.absolutePath}.
                    Please set the konan.home system property to specify the distribution path manually.
                """.trimIndent()
        }

        // The compiler jar is located in <dist>/konan/lib.
        return jarPath.parentFile.parentFile.parentFile
    }

    /**
     * Path to the current Kotlin/Native distribution
     *
     * - If one of `kotlin.native.home`, `org.jetbrains.kotlin.native.home`, `konan.home` is provided,
     *   it's value used
     * - Otherwise determines a path to a jar containing this class.
     *
     * @throws IllegalStateException when cannot find Kotlin/Native distribution
     */
    val homePath: File
        get() = kotlinNativeHome?.let {
            // KT-58979: KonanLibraryImpl needs normalized klib paths to correctly provide symbols from resolved klibs
            File(it).normalize()
        } ?: defaultHomePath()
}