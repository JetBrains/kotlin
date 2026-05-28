/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.javaInterop

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeParameter
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.useSiteModule


/**
 * Converts the given [KaClassSymbol] to Java [PsiClass] in the context of the [useSiteModule].
 *
 * The resulting [PsiClass] is the view on the given Kotlin class from Java.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiClass] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaClassSymbol] is not visible from Java, returns `null`.
 *
 * ### Example:
 * The following Kotlin class:
 * ```kotlin
 * interface MyInterface {
 *     val property: String
 *
 *     fun function(argument: Int): Int
 * }
 * ```
 *
 * Is seen as the following [PsiClass] from Java:
 * ```java
 * public interface MyInterface {
 *     @org.jetbrains.annotations.NotNull()
 *     java.lang.String getProperty();
 *
 *     int function(int argument);
 * }
 * ```
 *
 * The following Kotlin enum class:
 * ```kotlin
 * package example
 *
 * enum class MyEnum {
 *     A {
 *         fun foo() {}
 *     },
 *     B,
 * }
 * ```
 *
 * Is seen as the following [PsiClass] from Java:
 * ```java
 * public enum MyEnum {
 *     A // PsiField
 *     {
 *         A();                     // Anonymous initializer PsiClass
 *         public final void foo(); //
 *     },
 *
 *     B; // Does not have an anonymous initializer class, just PsiField
 *
 *     public static kotlin.enums.EnumEntries<example.MyEnum> getEntries();
 *     public static example.MyEnum [] values();
 *     public static example.MyEnum valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;
 *     private  MyEnum();
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaClassSymbol.asPsiClass(): PsiClass? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiClass(this)
}

/**
 * Converts the given [KaFileSymbol] to Java facade [PsiClass] in the context of the [useSiteModule].
 *
 * The resulting [PsiClass] is the view on the given Kotlin file from Java. E.g., `main.kt` file is converted to `MainKt` class facade.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiClass] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * Note that the produced facade class only stores non-class declarations. Each Kotlin class is mapped to its own top-level [PsiClass].
 *
 * If [useSiteModule] is not a JVM module or the provided [KaFileSymbol] is not supported, returns `null`.
 *
 * Examples of non-supported files are:
 * - Scripts
 * - Files with no top-level callables
 *
 * ### Example:
 * The following Kotlin file:
 * ```kotlin
 * // MyFile.kt
 * class MyClass
 *
 * fun foo(t: Int) {}
 *
 * val x: Int = 0
 * ```
 *
 * Is seen as the following [PsiClass] from Java:
 * ```java
 * public final class MyFileKt { // Doesn't contain `MyClass` declaration
 *     private static final int x = 0;
 *
 *     public static int getX();
 *
 *     public static void foo(int);
 * }
 * ```
 *
 * @see KaScriptSymbol.asFacadePsiClass
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaFileSymbol.asFacadePsiClass(): PsiClass? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asFacadePsiClass(this)
}

/**
 * Converts the given [KaScriptSymbol] to Java facade [PsiClass] in the context of the [useSiteModule].
 *
 * The resulting [PsiClass] is the view on the given Kotlin script from Java.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiClass] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * In contrast to [KaFileSymbol.asFacadePsiClass], regular Kotlin classes in scripts are mapped to nested [PsiClass]es.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaScriptSymbol] comes from a code fragment, returns `null`.
 *
 * ### Example:
 * The following Kotlin script:
 * ```kotlin
 * // MyScript.kts
 * println("Hello, World!")
 *
 * val x = 1
 *
 * class MyClass {
 *     fun bar() {}
 * }
 * ```
 *
 * Is seen as the following [PsiClass] from Java:
 * ```java
 * public final class MyScript extends kotlin.script.templates.standard.ScriptTemplateWithArgs {
 *     public static void main(java.lang.String[]);
 *
 *     public MyScript(java.lang.String[]);
 *
 *     private final int x = 1;
 *     public int getX();
 *
 *     public static final class MyClass {
 *         public MyClass();
 *         public void bar();
 *     }
 * }
 * ```
 *
 * @see KaFileSymbol.asFacadePsiClass
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaScriptSymbol.asFacadePsiClass(): PsiClass? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asFacadePsiClass(this)
}

/**
 * Converts the given [KaFunctionSymbol] to Java [PsiMethod]s in the context of the [useSiteModule].
 *
 * The resulting list is the view on the given Kotlin declaration from Java and contains all [PsiMethod]s produced by [this].
 *
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced list is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaFunctionSymbol] is not visible from Java, returns an empty list.
 *
 * ### Example:
 * The following Kotlin function:
 * ```kotlin
 * // MyFile.kt
 * @JvmOverloads
 * @JvmName("jvmFoo")
 * fun foo(a: Int, b: Int = 1) {}
 * ```
 *
 * Is seen as the following [PsiMethod]s from Java:
 * ```java
 * public final class MyFileKt {
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static void jvmFoo(int);
 *
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static void jvmFoo(int, int);
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaFunctionSymbol.asPsiMethods(): List<PsiMethod> {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiMethods(this)
}

/**
 * Converts the given [KaTypeParameterSymbol] to Java [PsiTypeParameter]s in the context of the [useSiteModule].
 *
 * The resulting list is the view on the given Kotlin type parameter from Java and contains all [PsiTypeParameter]s produced by [this].
 * Multiple type parameters might be produced when the enclosing Kotlin declaration is mapped to multiple Java PSI declarations.
 *
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced list is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaTypeParameterSymbol] is not visible from Java, returns an empty list.
 *
 * ### Example:
 * The following Kotlin type parameter `T`:
 * ```kotlin
 * // MyFile.kt
 * @JvmOverloads
 * @JvmName("jvmFoo")
 * fun <T> foo(a: T, b: Int = 1) {}
 * ```
 *
 * Is seen as the following [PsiTypeParameter]s from Java:
 * ```java
 * public final class MyFileKt {
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static final <T> void jvmFoo(T a);
 * //                      ^^^
 * //                  PsiTypeParameter
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static final <T> void jvmFoo(T a, int b);
 * //                      ^^^
 * //                  PsiTypeParameter
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaTypeParameterSymbol.asPsiTypeParameters(): List<PsiTypeParameter> {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiTypeParameters(this)
}

/**
 * Converts the given [KaParameterSymbol] to Java [PsiParameter]s in the context of the [useSiteModule].
 *
 * The resulting list is the view on the given Kotlin parameter from Java and contains all [PsiParameter]s produced by [this].
 * Multiple parameters might be produced when the enclosing Kotlin declaration is mapped to multiple Java PSI declarations.
 *
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced list is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaParameterSymbol] is not visible from Java, returns an empty list.
 *
 * ### Example:
 * The following Kotlin parameter `a`:
 * ```kotlin
 * // MyFile.kt
 * @JvmOverloads
 * @JvmName("jvmFoo")
 * fun foo(a: Int, b: Int = 1) {}
 * ```
 *
 * Is seen as the following [PsiParameter]s from Java:
 * ```java
 * public final class MyFileKt {
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static void jvmFoo(int a);
 * //                               ^^^
 * //                          PsiParameter
 *     @kotlin.jvm.JvmName(name = "jvmFoo")
 *     @kotlin.jvm.JvmOverloads()
 *     public static void jvmFoo(int a, int b);
 * //                               ^^^
 * //                          PsiParameter
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaParameterSymbol.asPsiParameters(): List<PsiParameter> {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiParameters(this)
}

/**
 * Converts the given [KaBackingFieldSymbol] to Java [PsiField] in the context of the [useSiteModule].
 *
 * The resulting [PsiField] is the view on the given Kotlin backing field from Java.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiField] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaBackingFieldSymbol] is not visible from Java as a [PsiField],
 * returns `null`.
 *
 * ### Example:
 * The following Kotlin property with implicit backing field:
 * ```kotlin
 * // MyFile.kt
 * val x: Int = 0 // implicit backing field
 * ```
 *
 * Is seen as the following [PsiMethod] getter and [PsiField] from Java:
 * ```java
 * public final class MyFileKt {
 *     private static final int x = 0; // PsiField
 *
 *     public static int getX();
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaBackingFieldSymbol.asPsiField(): PsiField? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiField(this)
}

/**
 * Converts the given [KaClassSymbol] to Java [PsiField] in the context of the [useSiteModule].
 *
 * The resulting [PsiField] is the view on the given Kotlin declaration from Java.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiField] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * [KaClassSymbol] has be mapped to [PsiField] in several cases:
 * - `INSTANCE` field for object declarations
 * - `Companion` (or the given name for named companions) field for companion objects
 *
 * If [useSiteModule] is not a JVM module or the provided [KaClassSymbol] is not visible from Java as a [PsiField],
 * returns `null`.
 *
 * ### Example:
 * The following Kotlin companion object
 * ```kotlin
 * package example
 *
 * class MyClass {
 *     companion object {
 *         fun foo() {}
 *     }
 * }
 * ```
 *
 * Is seen as the following [PsiField] from Java:
 * ```java
 * public final class MyClass {
 *     public static final example.MyClass.Companion Companion; // `Companion` instance field
 *
 *     public MyClass();
 *
 *     public static final class Companion {
 *         private  Companion();
 *
 *         public final void foo();
 *     }
 * }
 * ```
 *
 * The following Kotlin object
 * ```kotlin
 * package example
 *
 * object MyObject {
 *     fun foo() {}
 * }
 * ```
 *
 * Is seen as the following [PsiField] from Java:
 * ```java
 * public final class MyObject {
 *     public static final example.MyObject INSTANCE; // `INSTANCE` field
 *
 *     private MyObject();
 *
 *     public void foo();
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaClassSymbol.asPsiField(): PsiField? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiField(this)
}

/**
 * Converts the given [KaEnumEntrySymbol] to Java [PsiEnumConstant] in the context of the [useSiteModule].
 *
 * The resulting [PsiEnumConstant] is the view on the given Kotlin enum entry from Java.
 * [useSiteModule] for the current [KaSession] is used to provide proper actualizations for `expect` declarations.
 * Since the produced [PsiEnumConstant] is a JVM-specific representation, [useSiteModule] is expected to be a JVM one.
 *
 * If [useSiteModule] is not a JVM module or the provided [KaEnumEntrySymbol] is not visible from Java as a [PsiEnumConstant],
 * returns `null`.
 *
 * ### Example:
 * The following Kotlin enum entries
 * ```kotlin
 * enum class MyEnum {
 *     A,
 *     B
 * }
 * ```
 *
 * Are seen as the following [PsiEnumConstant]s from Java:
 * ```java
 * public enum MyEnum {
 *     A, // PsiEnumConstant for MyEnum.A
 *     B; // PsiEnumConstant for MyEnum.B
 *
 *     public static kotlin.enums.EnumEntries<example.MyEnum> getEntries();
 *     public static example.MyEnum [] values();
 *     public static example.MyEnum valueOf(java.lang.String) throws java.lang.IllegalArgumentException, java.lang.NullPointerException;
 *     private  MyEnum();
 * }
 * ```
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaEnumEntrySymbol.asPsiField(): PsiEnumConstant? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asPsiField(this)
}
