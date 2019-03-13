/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container

import java.lang.IllegalStateException

/**
 * This is a marker-interface for components which are needed for common resolve
 * facilities (like resolve, or deserialization), but are platform-specific.
 *
 * PlatformSpecificExtensions has to be present in the container in exactly one
 * instance (hence common pattern with providing no-op DEFAULT/EMPTY implementation
 * in the corresponding interface)
 *
 * In multiplatform modules such components require special treatment. Namely,
 * if several components of the same type are provided, then it's not an illegal state;
 * rather, we have to carefully resolve clash on case-by-case basis.
 * See [PlatformExtensionsClashResolver] also
 *
 * Example: [org.jetbrains.kotlin.resolve.IdentifierChecker]. It is used in platform-agnostic code,
 * which resolves and checks identifiers for correctness. Each platform has it's own rules
 * regarding identifier correctness. In MPP modules we can't choose only one IdentifierChecker;
 * instead, we have to provide a "composite" IdentifierChecker which will launch checks of *each*
 * platform.
 */
interface PlatformSpecificExtension<S : PlatformSpecificExtension<S>>

abstract class PlatformExtensionsClashResolver<E : PlatformSpecificExtension<E>>(val applicableTo: Class<E>) {
    abstract fun resolveExtensionsClash(extensions: List<E>): E

    abstract class PreferNonDefault<E : PlatformSpecificExtension<E>>(
        private val defaultValue: E,
        applicableTo: Class<E>
    ) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E {
            val nonDefaultExtensions = extensions.filter { it != defaultValue }

            return when (nonDefaultExtensions.size) {
                0 -> defaultValue
                1 -> nonDefaultExtensions.single()
                else -> throw IllegalStateException("Can't resolve clash, several non-default extensions provided: ${extensions.joinToString()}")
            }
        }
    }

    abstract class UseAnyOf<E : PlatformSpecificExtension<E>>(
        private val value: E,
        applicableTo: Class<E>
    ) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E {
            return when {
                extensions.any { it == value } -> value
                extensions.size == 1 -> extensions.single()
                else -> throw IllegalStateException("Can't resolve clash, several non-default extensions provided: ${extensions.joinToString()}")
            }
        }
    }
}

