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
 * The [visibility](https://kotlinlang.org/docs/visibility-modifiers.html) of a [KaSymbol]. As symbols can represent both Kotlin and Java
 * declarations, [KaSymbolVisibility] covers both visibility definitions.
 *
 * In addition, the following articles contain information about how visibility is affected by Kotlin and Java interoperability:
 *
 * - [Calling Java from Kotlin](https://kotlinlang.org/docs/java-interop.html)
 * - [Calling Kotlin from Java](https://kotlinlang.org/docs/java-to-kotlin-interop.html#visibility)
 */
public enum class KaSymbolVisibility {
    /**
     * A *public* declaration is visible everywhere. This is the default visibility in Kotlin.
     *
     * ```kotlin
     * fun publicFunction() {} // public
     * ```
     */
    PUBLIC,

    /**
     * A *protected* declaration is visible inside its containing declaration and all its members, as well as in subclasses.
     *
     * ```kotlin
     * abstract class BaseClass { // public
     *     protected abstract doSmth() // protected
     *
     *     fun publicApi() { // public
     *         doSmth()
     *     }
     * }
     * ```
     */
    PROTECTED,

    /**
     * An *internal* declaration is visible everywhere in the same module. If the declaration is a class member, the class must also be
     * visible for the internal member to be visible.
     *
     * ```kotlin
     * internal fun internalFunction() {} // internal
     * ```
     */
    INTERNAL,

    /**
     * A *package-protected* Java declaration is visible in its class, package, and in all subclasses (even outside the package).
     *
     * Unlike Kotlin, `protected` visibility in Java allows usages not only from inherited classes, but also from other classes in the same
     * package. Effectively, it is a union of [PROTECTED] and [PACKAGE_PRIVATE].
     *
     * ```java
     * public class JavaClass { // public
     *     protected void packageProtectedMember() { // package-protected
     *     }
     *
     *     protected static void packageProtectedStaticMember() { // package-protected
     *     }
     * }
     * ```
     */
    PACKAGE_PROTECTED,

    /**
     * A *package-private* Java declaration is visible in its class and package. This is the default visibility in Java.
     *
     * ```java
     * class JavaClass { // package-private
     *     void foo() { // package-private
     *     }
     * }
     * ```
     */
    PACKAGE_PRIVATE,

    /**
     * A *private* declaration is visible inside its containing declaration and all its members, or inside the whole file if the declaration
     * is top-level.
     *
     * ```kotlin
     * private fun privateFunction() {} // private (visible in the file)
     *
     * class Foo {
     *     private fun privateMember() {} // private (visible in `Foo`)
     * }
     * ```
     */
    PRIVATE,

    /**
     * The visibility of local declarations.
     *
     * ```kotlin
     * fun publicFunction() { // public
     *     val localProperty = 0 // local
     *     fun localFunction() { } // local
     * }
     */
    LOCAL,

    /**
     * An unknown visibility, for example in the case of erroneous code where it is impossible to infer the proper visibility.
     */
    UNKNOWN,
}

/**
 * Converts the Kotlin compiler's [Visibility] to the Analysis API's [KaSymbolVisibility].
 */
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
