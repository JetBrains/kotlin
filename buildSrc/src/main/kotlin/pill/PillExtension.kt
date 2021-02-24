/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtension {
    enum class Variant {
        // Default variant (./gradlew pill)
        BASE() {
            override val includes = setOf(BASE)
        },

        // Full variant (./gradlew pill -Dpill.variant=full)
        FULL() {
            override val includes = setOf(BASE, FULL)
        },

        // Do not import the project to JPS model, but set some options for it
        NONE() {
            override val includes = emptySet<Variant>()
        },

        // 'BASE' if the "jps-compatible" plugin is applied, 'NONE' otherwise
        DEFAULT() {
            override val includes = emptySet<Variant>()
        };

        abstract val includes: Set<Variant>
    }

    open var variant: Variant = Variant.DEFAULT

    open var excludedDirs: List<File> = emptyList()

    @Suppress("unused")
    fun Project.excludedDirs(vararg dirs: String) {
        excludedDirs = excludedDirs + dirs.map { File(projectDir, it) }
    }

    @Suppress("unused")
    fun serialize() = mapOf<String, Any?>(
        "variant" to variant.name,
        "excludedDirs" to excludedDirs
    )
}