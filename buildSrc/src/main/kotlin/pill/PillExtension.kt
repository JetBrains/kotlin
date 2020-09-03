/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtension {
    /*
     * Here's how you can specify a custom variant:
     * `./gradlew pill -Dpill.variant=<NAME>`
     */
    enum class Variant {
        BASE, // Includes compiler and IDE (default)
        FULL, // Includes compiler, IDE and Gradle plugin
    }

    open var variant: Variant? = null

    open var excludedDirs: List<File> = emptyList()

    @Suppress("unused")
    fun Project.excludedDirs(vararg dirs: String) {
        excludedDirs = excludedDirs + dirs.map { File(projectDir, it) }
    }

    @Suppress("unused")
    fun serialize() = mapOf<String, Any?>(
        "variant" to variant?.name,
        "excludedDirs" to excludedDirs
    )
}