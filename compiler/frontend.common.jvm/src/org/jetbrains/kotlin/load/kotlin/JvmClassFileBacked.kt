/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

/**
 * An entity backed by a JVM class file, such as a binary class or the container source of a declaration from it.
 */
interface JvmClassFileBacked {
    /**
     * The major version of the class file, or `null` if there is no such class file or its version is unknown.
     */
    val classFileMajorVersion: Int?
}
