/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * The naming convention for JVM classes that hold the top-level declarations of Kotlin files.
 */
object JvmFileClassNaming {
    private const val FILE_CLASS_NAME_SUFFIX = "Kt"
    private const val MULTIFILE_CLASS_PART_NAME_DELIMITER = "__"

    private val INVALID_JAVA_IDENTIFIER_CHARACTERS = "[^\\p{L}\\d]".toRegex()

    /**
     * Returns the file class name for a file name: `foo.kt` gives `FooKt`. Characters that are not letters or digits are replaced with
     * `_`, and `_` is prepended if the result cannot start a Java identifier, so `foo-bar.kt` gives `Foo_barKt` and `1.kt` gives `_1Kt`.
     *
     * A compiled file ([isCompiled]) is already named after its class, so `FooKt.class` gives `FooKt`.
     */
    @JvmStatic
    @JvmOverloads
    fun fileClassName(fileName: String, isCompiled: Boolean = false): String {
        val nameWithoutExtension = fileName.substringBeforeLast('.')

        return if (isCompiled) {
            nameWithoutExtension
        } else {
            capitalizeAsJavaClassName(nameWithoutExtension) + FILE_CLASS_NAME_SUFFIX
        }
    }

    /**
     * Returns the fully qualified [file class name][fileClassName] for a file from the [packageFqName] package.
     */
    @JvmStatic
    @JvmOverloads
    fun fileClassFqName(packageFqName: FqName, fileName: String, isCompiled: Boolean = false): FqName {
        return packageFqName.child(Name.identifier(fileClassName(fileName, isCompiled)))
    }

    /**
     * Returns the name of the class generated for a file that is a part of the [facadeClassName] multifile class: `Utils` and `foo.kt`
     * give `Utils__FooKt`.
     */
    @JvmStatic
    fun multifileClassPartName(facadeClassName: String, fileName: String): String {
        return facadeClassName + MULTIFILE_CLASS_PART_NAME_DELIMITER + fileClassName(fileName)
    }

    private fun capitalizeAsJavaClassName(name: String): String {
        if (name.isEmpty()) {
            return "_"
        }

        val sanitizedName = INVALID_JAVA_IDENTIFIER_CHARACTERS.replace(name, "_")

        // 'uppercase()' uses 'Locale.ROOT', so the result is locale-independent
        return if (Character.isJavaIdentifierStart(sanitizedName[0])) {
            sanitizedName[0].uppercase() + sanitizedName.substring(1)
        } else {
            "_$sanitizedName"
        }
    }
}
