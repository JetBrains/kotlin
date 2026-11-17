/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object JvmClassFileNames {
    const val CLASS_FILE_EXTENSION: String = "class"

    /** The name of the file of the class [relativeClassName] in its package directory, without extension, e.g. `A$B` for `A.B`. */
    @JvmStatic
    fun fileNameWithoutExtension(relativeClassName: FqName): String = relativeClassName.asString().replace('.', '$')

    /** The name of the file of the class [relativeClassName] in its package directory, e.g. `A$B.class` for `A.B`. */
    @JvmStatic
    @JvmOverloads
    fun classFileName(relativeClassName: FqName, extension: String = CLASS_FILE_EXTENSION): String =
        fileName(fileNameWithoutExtension(relativeClassName), extension)

    /**
     * The name of the file of the class [nestedClassName] nested in the class of the file [outerFileNameWithoutExtension], without
     * extension, e.g. `A$B$C` for `C` in `A$B`.
     */
    @JvmStatic
    fun nestedClassFileNameWithoutExtension(outerFileNameWithoutExtension: String, nestedClassName: Name): String =
        outerFileNameWithoutExtension + '$' + nestedClassName.asString()

    @JvmStatic
    fun packageDirectoryName(name: Name): String? = name.identifierOrNullIfSpecial

    @JvmStatic
    fun fileName(nameWithoutExtension: String, extension: String): String = "$nameWithoutExtension.$extension"
}
