/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.utf8

import kotlin.test.*
import kotlin.reflect.KClass

// region Util
fun assertEquals(expected: ByteArray, actual: ByteArray, message: String) =
        assertTrue(expected.contentEquals(actual), message)

fun checkUtf16to8(string: String, expected: IntArray, conversion: String.() -> ByteArray) {
    expected.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val expectedBytes = ByteArray(expected.size) { i -> expected[i].toByte() }
    val actual = string.conversion()
    assertEquals(expectedBytes, actual, """
        Assert failed for string: $string
        Expected: ${expected.joinToString()}
        Actual: ${actual.joinToString()}
    """.trimIndent())
}

fun checkUtf16to8Replacing(string: String, expected: IntArray) = checkUtf16to8(string, expected) { toUtf8() }
fun checkUtf16to8Throwing(string: String, expected: IntArray) = checkUtf16to8(string, expected) { toUtf8OrThrow() }

fun checkUtf16to8Replacing(string: String, expected: IntArray, start: Int, size: Int) =
        checkUtf16to8(string, expected) { toUtf8(start, size) }

fun checkUtf16to8Throwing(string: String, expected: IntArray, start: Int, size: Int) =
        checkUtf16to8(string, expected) { toUtf8OrThrow(start, size) }

fun checkUtf8to16(expected: String, array: IntArray, conversion: ByteArray.() -> String) {
    array.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val arrayBytes = ByteArray(array.size) { i -> array[i].toByte() }
    val actual = arrayBytes.conversion()
    assertEquals(expected, actual, """
        Assert failed for string: $expected
        Expected: $expected
        Actual: $actual
    """.trimIndent())
}

fun checkUtf8to16Replacing(expected: String, array: IntArray) = checkUtf8to16(expected, array) { stringFromUtf8() }
fun checkUtf8to16Throwing(expected: String, array: IntArray) = checkUtf8to16(expected, array) { stringFromUtf8OrThrow() }

fun checkUtf8to16Replacing(expected: String, array: IntArray, start: Int, size: Int) =
        checkUtf8to16(expected, array) { stringFromUtf8(start, size) }
fun checkUtf8to16Throwing(expected: String, array: IntArray, start: Int, size: Int) =
        checkUtf8to16(expected, array) { stringFromUtf8OrThrow(start, size) }

fun <T: Any> checkThrows(e: KClass<T>, string: String, action: () -> Unit) {
    var exception: Throwable? = null
    try {
        action()
    } catch (e: Throwable) {
        exception = e
    }
    assertNotNull(exception, "No excpetion was thrown for string: $string")
    assertTrue(e.isInstance(exception),"""
                Wrong exception was thrown for string: $string
                Expected: ${e.qualifiedName}
                Actual: ${exception::class.qualifiedName}
    """.trimIndent())
}

fun checkUtf16to8Throws(string: String) = checkThrows(IllegalCharacterConversionException::class, string) {
    string.toUtf8OrThrow()
}

fun checkUtf16to8Throws(string: String, start: Int, size: Int) =
        checkThrows(IllegalCharacterConversionException::class, string) {
            string.toUtf8OrThrow(start, size)
        }

fun checkUtf8to16Throws(string: String, array: IntArray)
        = checkThrows(IllegalCharacterConversionException::class, string) {
    array.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val arrayBytes = ByteArray(array.size) { i -> array[i].toByte() }
    arrayBytes.stringFromUtf8OrThrow()
}

fun checkUtf8to16Throws(string: String, array: IntArray, start: Int, size: Int)
        = checkThrows(IllegalCharacterConversionException::class, string) {
    array.forEach {
        assertTrue(it in Byte.MIN_VALUE..Byte.MAX_VALUE, "Expected array contains illegal values")
    }
    val arrayBytes = ByteArray(array.size) { i -> array[i].toByte() }
    arrayBytes.stringFromUtf8OrThrow(start, size)
}


fun checkDobuleConversionThrows(string: String) = checkThrows(NumberFormatException::class, string) {
    string.toDouble()
}

fun checkFloatConversionThrows(string: String) = checkThrows(NumberFormatException::class, string) {
    string.toFloat()
}
// endregion

// region UTF-16 -> UTF-8 conversion
fun test16to8() {
    // Test manual conversion with replacement.
    // Valid strings.
    checkUtf16to8Replacing("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkUtf16to8Replacing("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkUtf16to8Replacing("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkUtf16to8Replacing("", intArrayOf())
    // Illegal surrogate pair -> replace with default
    checkUtf16to8Replacing("\uDC00\uD800", intArrayOf(-17, -65, -67, -17, -65, -67))
    // Different kinds of input
    checkUtf16to8Replacing("\uD800\uDC001\uDC00\uD800",
            intArrayOf(-16, -112, -128, -128, '1'.toInt(), -17, -65, -67, -17, -65, -67))
    // Lone surrogate - replace with default
    checkUtf16to8Replacing("\uD80012", intArrayOf(-17, -65, -67, '1'.toInt(), '2'.toInt()))
    checkUtf16to8Replacing("\uDC0012", intArrayOf(-17, -65, -67, '1'.toInt(), '2'.toInt()))
    checkUtf16to8Replacing("12\uD800", intArrayOf('1'.toInt(), '2'.toInt(), -17, -65, -67))


    // Test manual conversion with an exception if an input is invalid.
    // Valid strings.
    checkUtf16to8Throwing("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkUtf16to8Throwing("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkUtf16to8Throwing("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkUtf16to8Replacing("", intArrayOf())

    // Illegal surrogate pair -> throw
    checkUtf16to8Throws("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkUtf16to8Throws("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkUtf16to8Throws("\uD80012")
    checkUtf16to8Throws("\uDC0012")
    checkUtf16to8Throws("12\uD800")

    // Test double parsing.
    assertEquals(4.2, "4.2".toDouble())
    // Illegal surrogate pair -> throw
    checkDobuleConversionThrows("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkDobuleConversionThrows("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkDobuleConversionThrows("\uD80012")
    checkDobuleConversionThrows("\uDC0012")
    checkDobuleConversionThrows("12\uD800")

    // Test float parsing.
    assertEquals(4.2F,  "4.2".toFloat())
    // Illegal surrogate pair -> throw
    checkFloatConversionThrows("\uDC00\uD800")
    // Different kinds of input (including illegal one) -> throw
    checkFloatConversionThrows("\uD800\uDC001\uDC00\uD800")
    // Lone surrogate - throw
    checkFloatConversionThrows("\uD80012")
    checkFloatConversionThrows("\uDC0012")
    checkFloatConversionThrows("12\uD800")
}

fun test16to8CustomBorders() {
    // Test manual conversion with replacement and custom borders.
    // Valid strings.
    checkUtf16to8Replacing("Hello!", intArrayOf('H'.toInt(), 'e'.toInt()), 0, 2)
    checkUtf16to8Replacing("Hello!", intArrayOf('e'.toInt(), 'l'.toInt()), 1, 2)
    checkUtf16to8Replacing("Hello!", intArrayOf('o'.toInt(), '!'.toInt()), 4, 2)
    checkUtf16to8Replacing("Hello!", intArrayOf(), 0, 0)
    checkUtf16to8Replacing("Hello!", intArrayOf(), 6, 0)

    checkUtf16to8Replacing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 0, 4)
    checkUtf16to8Replacing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 2, 4)
    checkUtf16to8Replacing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 4, 4)

    // Illegal surrogate pair -> replace with default
    checkUtf16to8Replacing("\uDC00\uD80012",
            intArrayOf(-17, -65, -67, -17, -65, -67, '1'.toInt()), 0, 3)
    checkUtf16to8Replacing("1\uDC00\uD8002",
            intArrayOf(-17, -65, -67, -17, -65, -67, '2'.toInt()), 1, 3)
    checkUtf16to8Replacing("12\uDC00\uD800",
            intArrayOf('2'.toInt(), -17, -65, -67, -17, -65, -67), 1, 3)

    // Lone surrogate - replace with default
    checkUtf16to8Replacing("1\uD800\uDC002", intArrayOf('1'.toInt(), -17, -65, -67), 0, 2)
    checkUtf16to8Replacing("1\uD800\uDC002", intArrayOf(-17, -65, -67, '2'.toInt()), 2, 2)

    // Index out of bound
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8(-1, 4)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8(5, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8(2, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8(3, -2)}


    // Test manual conversion with an exception if an input is invalid and custom borders.
    // Valid strings.
    checkUtf16to8Throwing("Hello!", intArrayOf('H'.toInt(), 'e'.toInt()), 0, 2)
    checkUtf16to8Throwing("Hello!", intArrayOf('e'.toInt(), 'l'.toInt()), 1, 2)
    checkUtf16to8Throwing("Hello!", intArrayOf('o'.toInt(), '!'.toInt()), 4, 2)
    checkUtf16to8Throwing("Hello!", intArrayOf(), 0, 0)
    checkUtf16to8Throwing("Hello!", intArrayOf(), 6, 0)

    checkUtf16to8Throwing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 0, 4)
    checkUtf16to8Throwing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 2, 4)
    checkUtf16to8Throwing("\uD800\uDC00\uD800\uDC00\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128), 4, 4)

    // Illegal surrogate pair -> throw
    checkUtf16to8Throws("\uDC00\uD80012", 0, 3)
    checkUtf16to8Throws("1\uDC00\uD8002", 1, 3)
    checkUtf16to8Throws("12\uDC00\uD800", 1, 3)

    // Lone surrogate -> throw
    checkUtf16to8Throws("1\uD800\uDC002", 0, 2)
    checkUtf16to8Throws("1\uD800\uDC002",  2, 2)

    // Index out of bound
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8OrThrow(-1, 4)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8OrThrow(5, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8OrThrow(2, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { "Hello".toUtf8OrThrow(3, -2)}
}

fun testPrint() {
    // Valid strings.
    println("Hello")
    println("Привет")
    println("\uD800\uDC00")
    println("")

    // Illegal surrogate pair -> default output
    println("\uDC00\uD800")
    // Lone surrogate -> default output
    println("\uD80012")
    println("\uDC0012")
    println("12\uD800")

    // https://github.com/JetBrains/kotlin-native/issues/1091
    val array = byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x98.toByte(), 0xA5.toByte())
    val badStr = array.stringFromUtf8()
    assertEquals(2, badStr.length)
    println(badStr)
}
// endregion

// region UTF-8 -> UTF-16 conversion
fun test8to16() {
    // Test manual conversion with replacement.
    // Valid strings.
    checkUtf8to16Replacing("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkUtf8to16Replacing("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkUtf8to16Replacing("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkUtf8to16Replacing("", intArrayOf())

    // Incorrect UTF-8 lead character.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-1, '1'.toInt()))

    // Incomplete codepoint.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt()))
    checkUtf8to16Replacing("\uFFFD1\uFFFD", intArrayOf(-16, -97, -104, '1'.toInt(), -16, -97, -104))

    // Test manual conversion with exception throwing
    // Valid strings.
    checkUtf8to16Throwing("Hello", intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()))
    checkUtf8to16Throwing("Привет", intArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126))
    checkUtf8to16Throwing("\uD800\uDC00", intArrayOf(-16, -112, -128, -128))
    checkUtf8to16Throwing("", intArrayOf())

    // Incorrect UTF-8 lead character -> throw.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-1, '1'.toInt()))

    // Incomplete codepoint -> throw.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt()))
    checkUtf8to16Throws("\uFFFD1\uFFFD", intArrayOf(-16, -97, -104, '1'.toInt(), -16, -97, -104))
}

fun test8to16CustomBorders() {
    // Conversion with replacement.
    checkUtf8to16Replacing("He",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()),0, 2)
    checkUtf8to16Replacing("ll",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 2, 2)
    checkUtf8to16Replacing("lo",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 3, 2)
    checkUtf8to16Replacing("",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 0, 0)
    checkUtf8to16Replacing("",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 10, 0)

    checkUtf8to16Replacing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            0, 8)
    checkUtf8to16Replacing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            4, 8)
    checkUtf8to16Replacing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            8, 8)

    // Incorrect UTF-8 lead character.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-1, '1'.toInt(), '2'.toInt()), 0, 2)
    checkUtf8to16Replacing("\uFFFD2", intArrayOf('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 2)
    checkUtf8to16Replacing("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -1), 1, 2)

    // Incomplete codepoint.
    checkUtf8to16Replacing("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
    checkUtf8to16Replacing("\uFFFD2", intArrayOf('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 4)
    checkUtf8to16Replacing("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 4)


    // Index out of bound
    val helloArray = byteArrayOf('H'.toByte(), 'e'.toByte(), 'l'.toByte(), 'l'.toByte(), 'o'.toByte())
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8(-1, 4)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8(5, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8(2, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8(3, -2)}

    // Conversion with throwing
    checkUtf8to16Throwing("He",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()),0, 2)
    checkUtf8to16Throwing("ll",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 2, 2)
    checkUtf8to16Throwing("lo",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 3, 2)
    checkUtf8to16Throwing("",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 0, 0)
    checkUtf8to16Throwing("",
            intArrayOf('H'.toInt(), 'e'.toInt(), 'l'.toInt(), 'l'.toInt(), 'o'.toInt()), 10, 0)

    checkUtf8to16Throwing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            0, 8)
    checkUtf8to16Throwing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            4, 8)
    checkUtf8to16Throwing("\uD800\uDC00\uD800\uDC00",
            intArrayOf(-16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128, -16, -112, -128, -128),
            8, 8)

    // Incorrect UTF-8 lead character.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-1, '1'.toInt(), '2'.toInt()), 0, 2)
    checkUtf8to16Throws("\uFFFD2", intArrayOf('1'.toInt(), -1, '2'.toInt(), '3'.toInt()), 1, 2)
    checkUtf8to16Throws("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -1), 1, 2)

    // Incomplete codepoint.
    checkUtf8to16Throws("\uFFFD1", intArrayOf(-16, -97, -104, '1'.toInt(), '2'.toInt()), 0, 4)
    checkUtf8to16Throws("\uFFFD2", intArrayOf('1'.toInt(), -16, -97, -104, '2'.toInt(), '3'.toInt()), 1, 4)
    checkUtf8to16Throws("2\uFFFD", intArrayOf('1'.toInt(), '2'.toInt(), -16, -97, -104),  1, 4)

    // Index out of bound
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8OrThrow(-1, 4)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8OrThrow(5, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8OrThrow(2, 10)}
    checkThrows(IndexOutOfBoundsException::class, "Hello") { helloArray.stringFromUtf8OrThrow(3, -2)}
}
// endregion

@Test fun runTest() {
    test16to8()
    test16to8CustomBorders()
    test8to16()
    test8to16CustomBorders()
    testPrint()
}
