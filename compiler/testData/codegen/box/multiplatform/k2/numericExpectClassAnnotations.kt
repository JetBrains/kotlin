// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: NATIVE
// ^`kotlinx.cinterop.convert` comes from Native runtime.
// ISSUE: KT-87284
// ALLOW_KOTLIN_PACKAGE
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor
// NATIVE_STANDALONE

// MODULE: stdlibextra
// FILE: Annotations.kt
package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(
	vararg val actualizations: KClass<*>,
)

// MODULE: main-common(stdlibextra)()()
// FILE: Common.kt

@kotlin.NumericClass(Long::class)
expect class NSInteger {
    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
    fun toFloat(): Float
    fun toDouble(): Double
}

expect fun acceptNSInteger(num: NSInteger)
expect fun getNSInteger(): NSInteger

expect fun acceptLong(num: Long)

var sum: Long = 0L

@kotlin.NumericClass(UInt::class)
expect value class SizeT {
    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
    fun toFloat(): Float
    fun toDouble(): Double

    fun toUByte(): UByte
    fun toUShort(): UShort
    fun toUInt(): UInt
    fun toULong(): ULong
}

expect fun acceptSizeT(num: SizeT)
expect fun getSize(): SizeT

expect fun acceptULong(length: ULong)

sealed class OverloadVariant {
    data object NSInteger : OverloadVariant()
    data object Long : OverloadVariant()
    data object Int : OverloadVariant()
}

fun acceptNSIntegerOrInt(num: NSInteger) = OverloadVariant.NSInteger
fun acceptNSIntegerOrInt(num: Int) = OverloadVariant.Int

fun acceptLongOrInt(num: Long) = OverloadVariant.Long
fun acceptLongOrInt(num: Int) = OverloadVariant.Int

fun common(): String {
    acceptNSInteger(10)
    acceptNSInteger(1_000_000_000_000L)
    acceptNSInteger(5 + 5 * 20)
    acceptLong(getNSInteger())
    acceptULong(getSize())
    acceptSizeT(30u)

    val _ignore1: OverloadVariant.Int = acceptNSIntegerOrInt(20)
    val _ignore2: OverloadVariant.Long = acceptLongOrInt(getNSInteger())

    return when {
        sum == 1_000_000_000_175L -> "OK"
        else -> "FAIL: sum = $sum"
    }
}

// MODULE: main(stdlibextra)()(main-common)
// FILE: Platform.kt

actual typealias NSInteger = Long

actual fun acceptNSInteger(num: NSInteger) {
    sum += num
}

actual fun getNSInteger(): NSInteger = 10L

actual fun acceptLong(num: Long) {
    sum += num
}

actual typealias SizeT = UInt

actual fun acceptSizeT(num: SizeT) {
    sum += num.toLong()
}

actual fun getSize() = 20.toUInt()

actual fun acceptULong(length: ULong) {
    sum += length.toLong()
}

fun box() = common()
