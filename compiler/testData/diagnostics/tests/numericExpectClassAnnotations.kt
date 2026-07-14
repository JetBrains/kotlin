// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87284
// ALLOW_KOTLIN_PACKAGE
// WITH_STDLIB
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor +MultiPlatformProjects

// FILE: Stdlib.kt
package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(
	vararg val actualizations: KClass<*>,
)

// FILE: Main.kt

@kotlin.NumericClass(Long::class)
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

fun acceptNSIntegerOrInt(num: NSInteger) {}
fun acceptNSIntegerOrInt(num: Int) {}

fun acceptULong(num: ULong) {}
fun acceptLong(num: Long) {}
fun acceptInt(num: Int) {}

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

fun acceptSizeT(num: SizeT) {}
expect fun getSizeT(): SizeT

fun main() {
    acceptNSInteger(10)
    acceptNSInteger(1_000_000_000_000L)
    acceptNSInteger(<!ARGUMENT_TYPE_MISMATCH!>30u<!>)

    acceptNSIntegerOrInt(20)

    acceptLong(getNSInteger())
    acceptULong(<!ARGUMENT_TYPE_MISMATCH!>getNSInteger()<!>)
    acceptInt(<!ARGUMENT_TYPE_MISMATCH!>getNSInteger()<!>)

    acceptSizeT(30u)
    acceptSizeT(<!ARGUMENT_TYPE_MISMATCH!>30<!>)

    acceptULong(getSizeT())
    acceptLong(<!ARGUMENT_TYPE_MISMATCH!>getSizeT()<!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral */
