/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.library.KotlinLibrary

class DeserializedSourceFile(
    val name_: String, val index: Int, val library: KotlinLibrary
) : SourceFile {
    override fun getName(): String? = name_

    override fun equals(other: Any?): Boolean {
        return other is DeserializedSourceFile && library == other.library && index == other.index
    }

    override fun hashCode(): Int {
        return library.hashCode() xor index
    }
}