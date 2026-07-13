/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol

/**
 * A [KaScope] containing *non-static* callable members (functions, properties, and constructors) and all classifier members
 * (classes and objects) of the given [KaDeclarationContainerSymbol]. The scope includes members inherited from the symbol's supertypes,
 * in addition to members which are declared explicitly inside the symbol's body.
 *
 * The member scope doesn't include [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters). For
 * a scope which contains synthetic properties, please refer to [syntheticJavaPropertiesScope].
 *
 * @see staticMemberScope
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.memberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.memberScope(this)
    }

/**
 * A [KaScope] containing the *static* members of the given [KaDeclarationContainerSymbol].
 *
 * The behavior of the scope differs based on whether the given [KaDeclarationContainerSymbol] is a Kotlin or Java class:
 *
 * - **Kotlin class:** The scope contains static callables (functions and properties) and classifiers (classes and objects) declared
 *   directly in the [KaDeclarationContainerSymbol]. Hence, the static member scope for Kotlin classes is equivalent to
 *   [staticDeclaredMemberScope].
 * - **Java class:** The scope contains static callables (functions and properties) declared in the [KaDeclarationContainerSymbol] or
 *   any of its superclasses (excluding static callables from super-interfaces), and classes declared directly in the
 *   [KaDeclarationContainerSymbol]. This follows Kotlin's rules about static inheritance in Java classes, where static callables are
 *   propagated from superclasses, but nested classes are not.
 *
 * #### Kotlin Example
 *
 * ```kotlin
 * abstract class A {
 *     class C1
 *     inner class D1
 *     object O1
 *
 *     // There is no way to declare a static callable in an abstract class, as only enum classes define additional static callables.
 * }
 *
 * class B : A() {
 *     class C2
 *     inner class D2
 *     object O2
 *     companion object {
 *         val baz: String = ""
 *     }
 * }
 * ```
 *
 * The static member scope of `B` contains the following symbols:
 *
 * ```
 * class C2
 * inner class D2
 * object O2
 * companion object
 * ```
 *
 * #### Java Example
 *
 * ```java
 * // SuperInterface.java
 * public interface SuperInterface {
 *     public static void fromSuperInterface() { }
 * }
 *
 * // SuperClass.java
 * public abstract class SuperClass implements SuperInterface {
 *     static class NestedSuperClass { }
 *     class InnerSuperClass { }
 *     public static void fromSuperClass() { }
 * }
 *
 * // FILE: JavaClass.java
 * public class JavaClass extends SuperClass {
 *     static class NestedClass { }
 *     class InnerClass { }
 *     public static void fromJavaClass() { }
 * }
 * ```
 *
 * The static member scope of `JavaClass` contains the following symbols:
 *
 * ```
 * public static void fromSuperClass()
 * public static void fromJavaClass()
 * static class NestedClass
 * class InnerClass
 * ```
 *
 * @see memberScope
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.staticMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.staticMemberScope(this)
    }

/**
 * A [KaScope] containing *all* members from [memberScope] and [staticMemberScope].
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.combinedMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.combinedMemberScope(this)
    }

/**
 * A [KaScope] containing the *non-static* callables (functions, properties, and constructors) and inner classes explicitly
 * declared in the given [KaDeclarationContainerSymbol].
 *
 * The declared member scope does not contain classifiers (including the companion object) except for inner classes. To retrieve the
 * classifiers declared in this [KaDeclarationContainerSymbol], please use the *static* declared member scope provided by
 * [staticDeclaredMemberScope].
 *
 * @see staticDeclaredMemberScope
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.declaredMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.declaredMemberScope(this)
    }

/**
 * A [KaScope] containing the *static* callables (functions and properties) and all classifiers (classes and objects) explicitly
 * declared in the given [KaDeclarationContainerSymbol].
 *
 * It is worth noting that, while Java classes may contain declarations of static callables freely, in Kotlin only enum classes define
 * static callables. Hence, for non-enum Kotlin classes, it is not expected that the static declared member scope will contain any
 * callables.
 *
 * @see declaredMemberScope
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.staticDeclaredMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.staticDeclaredMemberScope(this)
    }

/**
 * A [KaScope] containing *all* members explicitly declared in the given [KaDeclarationContainerSymbol].
 *
 * In contrast to [declaredMemberScope] and [staticDeclaredMemberScope], this scope contains both static and non-static members.
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.combinedDeclaredMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.combinedDeclaredMemberScope(this)
    }

/**
 * A [KaScope] containing synthetic callables (functions and properties) created by interface delegation.
 *
 * #### Example
 *
 * ```kotlin
 * interface I {
 *     val foo: Int get() = 2
 *     fun bar(): String
 * }
 *
 * class A(
 *     private val p: I
 * ) : I by p {
 *     val regularProperty: Int = 5
 * }
 * ```
 *
 * The delegated member scope for `A` has the following entries:
 *
 * ```
 * override val foo: kotlin.Int
 *   get()
 *
 * override fun bar(): kotlin.String
 * ```
 *
 * `regularProperty` is not contained in the delegated member scope because it is not a delegated property.
 */
context(session: KaSession)
public val KaDeclarationContainerSymbol.delegatedMemberScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.delegatedMemberScope(this)
    }

/**
 * A [KaScope] containing the top-level declarations (such as classes, functions and properties) in the given [KaFileSymbol].
 */
context(session: KaSession)
public val KaFileSymbol.fileScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.fileScope(this)
    }

/**
 * A [KaScope] containing all members of the package represented by the given [KaPackageSymbol], not including members of subpackages.
 */
context(session: KaSession)
public val KaPackageSymbol.packageScope: KaScope
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.scopeProvider.packageScope(this)
    }
