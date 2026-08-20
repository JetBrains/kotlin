// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: NATIVE
// ^`kotlinx.cinterop.convert` comes from Native runtime.
// ISSUE: KT-87284
// ALLOW_KOTLIN_PACKAGE
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor
// NATIVE_STANDALONE

// MODULE: stdlibextra
// FILE: Annotations.kt
package support

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(
	vararg val actualizations: KClass<*>,
)

// MODULE: main-common(stdlibextra)()()
// FILE: Common.kt

import kotlinx.cinterop.convert

@support.NumericClass(Long::class)
expect class NSInteger {
    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
    fun toFloat(): Float
    fun toDouble(): Double

    operator fun compareTo(other: NSInteger): Int
    operator fun compareTo(other: Long): Int
}

expect fun acceptNSInteger(num: NSInteger)
expect fun getNSInteger(): NSInteger

expect fun acceptLong(num: Long)

var sum: Long = 0L

@support.NumericClass(UInt::class)
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

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    val _ignore4: OverloadVariant.NSInteger = acceptNSIntegerOrInt(0.convert())

    var a: NSInteger = 10
    acceptNSInteger(a)
    acceptLong(a)
    a = 20
    acceptNSInteger(a)
    acceptLong(a)

    var b: Long = getNSInteger()
    acceptLong(b)
    b = getNSInteger()
    acceptLong(b)

    var c: ULong = getSize()
    acceptULong(c)
    c = getSize()
    acceptULong(c)

    return when {
        a <= 0 -> "FAIL: a == $a <= 0"
        sum == 1_000_000_000_295L -> "OK"
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
