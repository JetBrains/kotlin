/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation

/**
 * ImportTracker is used to track imports directives used in Kotlin for correct build scope expansion in IC during JPS build.
 * e.g. Removing of Java file, that Kotlin relies on by importing
 */
@DefaultImplementation(ImportTracker.DoNothing::class)
interface ImportTracker {

    /**
     * Report import directives, where FqName is [importedFqName].
     * Format of [importedFqName] class is "package.Outer.Inner"
     */
    fun report(filePath: String, importedFqName: String)

    object DoNothing : ImportTracker {
        override fun report(filePath: String, importedFqName: String) {}
    }
}