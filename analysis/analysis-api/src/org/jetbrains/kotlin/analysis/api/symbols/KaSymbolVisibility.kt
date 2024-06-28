/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities

/**
 * See [the official Kotlin documentation](https://kotlinlang.org/docs/visibility-modifiers.html),
 * [Calling Java from Kotlin](https://kotlinlang.org/docs/java-interop.html)
 * and [Calling Kotlin from Java](https://kotlinlang.org/docs/java-to-kotlin-interop.html) for more details.
 */
public enum class KaSymbolVisibility {
    /**
     * A default visibility in Kotlin which means that your declarations will be visible everywhere.
     * ```kotlin
     * fun publicFunction() {} // public
     * ```
     */
    PUBLIC,

    /**
     * The same as [PRIVATE], but will be also visible in subclasses.
     *
     * ```kotlin
     * abstract class BaseClass { // public
     *   protected abstract doSmth() // protected
     *
     *   fun publicApi() { // public
     *     doSmth()
     *   }
     * }
     * ```
     */
    PROTECTED,

    /**
     * A declaration with this visibility will be visible everywhere in the same module.
     *
     * ```kotlin
     * internal fun internalFunction() {} // internal
     * ```
     */
    INTERNAL,

    /**
     * Represents Java protected visibility.
     *
     * Unlike Kotlin, `protected` visibility in Java allow usages not only from inherited classes, but also
     * from other classes in the same package.
     * Effectively, it is a union of [PROTECTED] and [PACKAGE_PRIVATE].
     *
     * ```java
     * public class JavaClass { // public
     *   protected void packageProtectedMember() { // package-protected
     *   }
     *
     *   protected static void packageProtectedStaticMember() { // package-protected
     *   }
     * }
     * ```
     */
    PACKAGE_PROTECTED,

    /**
     * Represents Java package-private visibility.
     * This is a default visibility in Java.
     * ```java
     * class JavaClass { // package-private
     *   void foo() { // package-private
     *   }
     * }
     * ```
     */
    PACKAGE_PRIVATE,

    /**
     * A declaration with this visibility will be visible inside this declaration and all its members.
     *
     * ```kotlin
     * private fun privateFunction() {}
     * ```
     */
    PRIVATE,

    /**
     * Local declarations have this visibility.
     *
     * ```kotlin
     * fun publicFunction() { // public
     *   val localProperty = 0 // local
     *   fun localFunction() { } // local
     * }
     */
    LOCAL,

    /**
     * This visibility may appear in the case of error code, there it is impossible to infer the proper one.
     */
    UNKNOWN,
}

@KaExperimentalApi
public val Visibility.asKaSymbolVisibility: KaSymbolVisibility
    get() = when (this) {
        Visibilities.Public -> KaSymbolVisibility.PUBLIC
        Visibilities.Protected -> KaSymbolVisibility.PROTECTED
        Visibilities.Internal -> KaSymbolVisibility.INTERNAL
        JavaVisibilities.ProtectedAndPackage, JavaVisibilities.ProtectedStaticVisibility -> KaSymbolVisibility.PACKAGE_PROTECTED
        JavaVisibilities.PackageVisibility -> KaSymbolVisibility.PACKAGE_PRIVATE
        Visibilities.Private, Visibilities.PrivateToThis -> KaSymbolVisibility.PRIVATE
        Visibilities.Local -> KaSymbolVisibility.LOCAL
        else -> KaSymbolVisibility.UNKNOWN
    }
