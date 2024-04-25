// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// TODO: KT-67753
// ISSUE: KT-65841
// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION
// DUMP_IR

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: internal.kt

package kotlin.internal

internal annotation class ActualizeByJvmBuiltinProvider

// FILE: builtins.kt

// Some companion objects in builtin classes contain constants.
// Despite the fact it's possible to declare `val` in `expect` class and
// `const val` in actual, we need those constants in expect classes anyway
// to get rid of potential errors with metadata compilation.
@file:Suppress("EXPECTED_PROPERTY_INITIALIZER")

package kotlin

import kotlin.internal.ActualizeByJvmBuiltinProvider

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

// FILE: testCommon.kt

annotation class AnnotationWithInt(val value: Int)

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInCommon // Currently it doesn't work with FIR2IR_FAKE_OVERRIDE_GENERATION (KT-67753)

fun testStringPlusInCommon() = "asdf" + 42
fun anyInCommon() = Any()
fun throwableInCommon() = Throwable()
fun testIntArrayOf() = intArrayOf(1, 2, 3)

// MODULE: platform()()(common)
// FILE: testPlatform.kt

@AnnotationWithInt(Int.MAX_VALUE)
class TestClassInPlatform

fun any() = Any()
fun string() = String() + 1
fun boolean() = true
fun int() = 42
fun intArray() = intArrayOf(1, 2, 3)

fun box() = "OK"
