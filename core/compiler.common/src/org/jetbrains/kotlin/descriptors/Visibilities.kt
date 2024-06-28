/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

object Visibilities {
    object Private : Visibility("private", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    // K2 doesn't use this visibility, see KT-55446 for details
    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"

        override fun mustCheckInImports(): Boolean = true
    }

    object Protected : Visibility("protected", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Internal : Visibility("internal", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Public : Visibility("public", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Local : Visibility("local", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Inherited : Visibility("inherited", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for INHERITED visibility")
        }
    }

    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true

        override val externalDisplayName: String
            get() = "invisible (private in a supertype)"
    }

    object Unknown : Visibility("unknown", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for UNKNOWN visibility")
        }
    }

    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0)
        put(Internal, 1)
        put(Protected, 1)
        put(Public, 2)
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
        return visibility === Private || visibility === PrivateToThis
    }

    val DEFAULT_VISIBILITY = Public
}
