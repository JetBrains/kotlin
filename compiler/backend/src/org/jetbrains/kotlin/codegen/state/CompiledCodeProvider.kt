/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

/**
 * A provider for externally available bytecode.
 *
 * Used for bytecode inlining.
 */
fun interface CompiledCodeProvider {
    object Empty : CompiledCodeProvider {
        override fun getClassBytes(className: String): ByteArray? {
            return null
        }
    }

    /**
     * Returns the complete content of a '.class' file for the given class name in the internal ('com/app/Outer$Nested') format,
     * or `null` if the class bytes are not available.
     */
    fun getClassBytes(className: String): ByteArray?
}