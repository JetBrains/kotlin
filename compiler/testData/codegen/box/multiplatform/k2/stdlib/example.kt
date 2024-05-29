// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// ISSUE: KT-65841
// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION
// DUMP_IR

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: internal.kt

package kotlin.internal

@OptionalExpectation
internal expect annotation class ActualizeByJvmBuiltinProvider()

// FILE: builtins.kt

// Some companion objects in builtin classes contain constants.
// Despite the fact it's possible to declare `val` in `expect` class and
// `const val` in actual, we need those constants in expect classes anyway
// to get rid of potential errors with metadata compilation.
@file:Suppress("EXPECTED_PROPERTY_INITIALIZER")

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

annotation class OptionalExpectation

@ActualizeByJvmBuiltinProvider
expect interface Annotation

@ActualizeByJvmBuiltinProvider
expect open class Any() {
    public open operator fun equals(other: Any?): Boolean

    public open fun hashCode(): Int

    public open fun toString(): String
}

@ActualizeByJvmBuiltinProvider
expect class Boolean

@ActualizeByJvmBuiltinProvider
expect class Int {
    companion object {
        const val MIN_VALUE: Int = -2147483648
        const val MAX_VALUE: Int = 2147483647
    }
}

@ActualizeByJvmBuiltinProvider
expect class String

@ActualizeByJvmBuiltinProvider
public expect abstract class Enum<E : Enum<E>>(name: String, ordinal: Int) : Comparable<E> {
    override final fun compareTo(other: E): Int

    override final fun equals(other: Any?): Boolean

    override final fun hashCode(): Int
}

@ActualizeByJvmBuiltinProvider
public expect open class Throwable() {
    public open val message: String?
    public open val cause: Throwable?

    public constructor(message: String?)

    public constructor(cause: Throwable?)
}

@ActualizeByJvmBuiltinProvider
public expect class IntArray(size: Int) {
    @Suppress("WRONG_MODIFIER_TARGET")
    public inline constructor(size: Int, init: (Int) -> Int)
}

@ActualizeByJvmBuiltinProvider
public expect fun Any?.toString(): String

@ActualizeByJvmBuiltinProvider
public expect operator fun String?.plus(other: Any?): String

@ActualizeByJvmBuiltinProvider
public expect fun intArrayOf(vararg elements: Int): IntArray

@ActualizeByJvmBuiltinProvider
@SinceKotlin("1.1")
public expect inline fun <reified T : Enum<T>> enumValues(): Array<T>

@Target(AnnotationTarget.TYPE)
@MustBeDocumented
public annotation class ExtensionFunctionType

// FILE: testCommon.kt

enum class TestEnumInCommon {
    A, B, C
}

annotation class AnnotationWithInt(val value: Int)

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInCommon

fun testStringPlusInCommon() = "asdf" + 42
fun anyInCommon() = Any()
fun throwableInCommon() = Throwable()
fun testIntArrayOf() = intArrayOf(1, 2, 3)

// Reproduce `IrConstructorSymbolImpl is unbound` for explicitely declared `@ExtensionFunctionType`
typealias funWithSuspend = suspend Any.() -> Any

public expect interface ClassThatInheritsSyntheticFunction : () -> Any // Reproduce KT-68188

// MODULE: platform()()(common)

// FILE: testPlatform.kt

public actual interface ClassThatInheritsSyntheticFunction : () -> Any // Reproduce KT-68188

enum class TestEnumInPlatform {
    D, E, F
}

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInPlatform

fun any() = Any()
fun string() = String() + 1
fun boolean() = true
fun int() = 42
fun intArray() = intArrayOf(1, 2, 3)

fun initCauseInPlatform() = Throwable().initCause(Throwable()) // `initCause` is not visible in `common` but visible in `platform`

fun box() = "OK"
