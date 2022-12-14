/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

/**
 * Something capable of producing a mangled name for a Kotlin declaration.
 *
 * @param Declaration A class representing a Kotlin declaration.
 */
interface KotlinMangleComputer<Declaration : Any> {

    /**
     * Computes the mangled name of [declaration].
     *
     * @param declaration The Kotlin declaration to compute a mangle name for.
     * @return The mangled name of [declaration].
     */
    fun computeMangle(declaration: Declaration): String

    /**
     * Creates a copy of this mangle computer with a different mangle mode but otherwise the same state.
     *
     * Useful for temporarily switching the mangle mode.
     *
     * @param newMode The mangle mode to use in the new mangle computer.
     * @return A copy of this mangle computer.
     */
    fun copy(newMode: MangleMode): KotlinMangleComputer<Declaration>
}
