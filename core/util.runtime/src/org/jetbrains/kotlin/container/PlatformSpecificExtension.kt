/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container

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
 * See also [PlatformExtensionsClashResolver].
 */
interface PlatformSpecificExtension<S : PlatformSpecificExtension<S>>

/**
 * Allows to specify which [PlatformSpecificExtension] should be used if there were two or more registrations
 * for [applicableTo] class in the container.
 *
 * [PlatformExtensionsClashResolver] should be registred in the container via [useClashResolver]-extension.
 *
 * NB. YOU DON'T NEED this mechanism for the most popular case of "one or several default vs.
 * zero or one non-default". Just use [DefaultImplementation], and default instances will be automatically
 * discriminated (see respective KDoc).
 * Use [PlatformExtensionsClashResolver] only for cases when you need more invloved logic.
 *
 * Example: [org.jetbrains.kotlin.resolve.IdentifierChecker]. It is used in platform-agnostic code,
 * which resolves and checks identifiers for correctness. Each platform has it's own rules
 * regarding identifier correctness. In MPP modules we can't choose only one IdentifierChecker;
 * instead, we have to provide a "composite" IdentifierChecker which will launch checks of *each*
 * platform.
 *
 */
abstract class PlatformExtensionsClashResolver<E : PlatformSpecificExtension<E>>(val applicableTo: Class<E>) {
    abstract fun resolveExtensionsClash(extensions: List<E>): E

    class FallbackToDefault<E : PlatformSpecificExtension<E>>(
        private val defaultValue: E,
        applicableTo: Class<E>
    ) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E = defaultValue
    }
}

