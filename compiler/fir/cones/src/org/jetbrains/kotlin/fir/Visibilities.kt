/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

object Visibilities {
    object PRIVATE : Visibility("private", isPublicAPI = false)

    object PRIVATE_TO_THIS : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"
    }

    object PROTECTED : Visibility("protected", isPublicAPI = true)
    object INTERNAL : Visibility("internal", isPublicAPI = false)
    object PUBLIC : Visibility("public", isPublicAPI = true)
    object LOCAL : Visibility("local", isPublicAPI = false)
    object INVISIBLE_FAKE : Visibility("invisible_fake", isPublicAPI = false)
    object UNKNOWN : Visibility("unknown", isPublicAPI = false)

    @OptIn(ExperimentalStdlibApi::class)
    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PRIVATE_TO_THIS, 0)
        put(PRIVATE, 0);
        put(INTERNAL, 1);
        put(PROTECTED, 1);
        put(PUBLIC, 2);
    }

    fun compare(first: Visibility, second: Visibility): Int? {
        val result = first.compareTo(second)
        if (result != null) {
            return result
        }
        val oppositeResult = second.compareTo(first)
        return if (oppositeResult != null) {
            -oppositeResult
        } else null
    }

    internal fun compareLocal(first: Visibility, second: Visibility): Int? {
        if (first === second) return 0
        val firstIndex = ORDERED_VISIBILITIES[first]
        val secondIndex = ORDERED_VISIBILITIES[second]
        return if (firstIndex == null || secondIndex == null || firstIndex == secondIndex) {
            null
        } else firstIndex - secondIndex
    }

    fun isPrivate(visibility: Visibility): Boolean {
        return visibility === PRIVATE || visibility === PRIVATE_TO_THIS
    }

    val DEFAULT_VISIBILITY = PUBLIC
}
