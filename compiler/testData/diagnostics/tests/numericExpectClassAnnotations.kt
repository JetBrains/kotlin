// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87284
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +MultiPlatformProjects
// INFERENCE_HELPERS
// FIR_DUMP

// FILE: Stdlib.kt
package kotlin

import kotlin.reflect.KClass

enum class NumericVariant {
    Byte, Short, Int, Long,
    UByte, UShort, UInt, ULong,
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(
	vararg val actualizations: NumericVariant,
)

// FILE: Cinterop.kt
package kotlinx.cinterop

public inline fun <reified R : Any> Int.convert(): R = TODO()

// FILE: Main.kt

import kotlinx.cinterop.convert

@kotlin.NumericClass(kotlin.NumericVariant.Long)
expect class NSInteger {
    fun toByte(): Byte
    fun toShort(): Short
    fun toInt(): Int
    fun toLong(): Long
    fun toFloat(): Float
    fun toDouble(): Double
}

fun acceptNSInteger(num: NSInteger) {}
expect fun getNSInteger(): NSInteger

sealed class OverloadVariant {
    data object NSInteger : OverloadVariant()
    data object Long : OverloadVariant()
    data object Int : OverloadVariant()
}

fun acceptNSIntegerOrInt(num: NSInteger) = OverloadVariant.NSInteger
fun acceptNSIntegerOrInt(num: Int) = OverloadVariant.Int

fun acceptLongOrInt(num: Long) = OverloadVariant.Long
fun acceptLongOrInt(num: Int) = OverloadVariant.Int

fun Long.callOverLongOrInt() = OverloadVariant.Long
fun Int.callOverLongOrInt() = OverloadVariant.Int

fun acceptULong(num: ULong) {}
fun acceptLong(num: Long) {}
fun acceptInt(num: Int) {}

@kotlin.NumericClass(actualizations = [kotlin.NumericVariant.UInt])
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

fun acceptSizeT(num: SizeT) {}
expect fun getSizeT(): SizeT

fun produceNSInteger(): NSInteger = 30

fun requestConversionDuringImplicitTypes1() = acceptNSInteger(30)
fun requestConversionDuringImplicitTypes2() = acceptSizeT(30u)

fun main() {
    acceptNSInteger(10)
    acceptNSInteger(1_000_000_000_000L)
    acceptNSInteger(<!ARGUMENT_TYPE_MISMATCH!>30u<!>)

    val _ignore1: OverloadVariant.Int = acceptNSIntegerOrInt(20)
    val _ignore2: OverloadVariant.Long = acceptLongOrInt(getNSInteger())
    val _ignore3: OverloadVariant.Long = getNSInteger().callOverLongOrInt()
    val _ignore4: OverloadVariant.NSInteger = acceptNSIntegerOrInt(0.convert())

    acceptLong(getNSInteger())
    acceptULong(<!ARGUMENT_TYPE_MISMATCH!>getNSInteger()<!>)
    acceptInt(<!ARGUMENT_TYPE_MISMATCH!>getNSInteger()<!>)

    acceptSizeT(30u)
    acceptSizeT(<!ARGUMENT_TYPE_MISMATCH!>30<!>)

    acceptULong(getSizeT())
    acceptLong(<!ARGUMENT_TYPE_MISMATCH!>getSizeT()<!>)

    var a: NSInteger = 10
    acceptNSInteger(a)
    acceptLong(a)
    acceptInt(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    a = 20
    acceptNSInteger(a)
    acceptLong(a)
    acceptInt(<!ARGUMENT_TYPE_MISMATCH!>a<!>)

    var b: Long = getNSInteger()
    acceptLong(b)
    acceptNSInteger(b)
    b = getNSInteger()
    acceptLong(b)
    acceptNSInteger(b)

    var c: ULong = getSizeT()
    acceptULong(c)
    acceptSizeT(<!ARGUMENT_TYPE_MISMATCH!>c<!>)
    c = getSizeT()
    acceptULong(c)
    acceptSizeT(<!ARGUMENT_TYPE_MISMATCH!>c<!>)

    acceptNSInteger(produceNSInteger())

    acceptNSInteger(<!ARGUMENT_TYPE_MISMATCH!>id(1)<!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral */
