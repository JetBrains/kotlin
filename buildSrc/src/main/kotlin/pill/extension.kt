@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import java.io.File
import org.gradle.api.Project

open class PillExtension {
    enum class Variant {
        // Default variant (./gradlew pill)
        BASE() { override val includes = setOf(BASE) },

        // Full variant (./gradlew pill -Dpill.variant=full)
        FULL() { override val includes = setOf(BASE, FULL) },

        // Do not import the project to JPS model, but set some options for it
        NONE() { override val includes = emptySet<Variant>() },

        // 'BASE' if the "jps-compatible" plugin is applied, 'NONE' otherwise
        DEFAULT() { override val includes = emptySet<Variant>() };

        abstract val includes: Set<Variant>
    }

    open var variant: Variant = Variant.DEFAULT

    open var importAsLibrary: Boolean = false

    open var excludedDirs: List<File> = emptyList()

    fun Project.excludedDirs(vararg dirs: String) {
        excludedDirs = excludedDirs + dirs.map { File(projectDir, it) }
    }

    open var libraryPath: File? = null
        set(v) {
            importAsLibrary = true
            field = v
        }
}