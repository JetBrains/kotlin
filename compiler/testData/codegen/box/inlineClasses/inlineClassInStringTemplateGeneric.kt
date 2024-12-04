// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val value: T)

fun <T: Int> test1_1(z: Z<T>) = "$z"
fun <T: Int> test1_2(z: Z<T>) = "$z$z"
fun <T: Int> test1_many(z: Z<T>) = "$z $z $z"
fun <T: Int> test1_concat1(z: Z<T>) = "-" + z
fun <T: Int> test1_concat2(z: Z<T>) = "$z" + z
fun <T: Int> test1_concat3(z: Z<T>) = "-" + z + z

fun <T: Int> test2_1(z: Z<T>?) = "$z"
fun <T: Int> test2_2(z: Z<T>?) = "$z$z"
fun <T: Int> test2_many(z: Z<T>?) = "$z $z $z"
fun <T: Int> test2_concat1(z: Z<T>?) = "-" + z
fun <T: Int> test2_concat2(z: Z<T>?) = "$z" + z
fun <T: Int> test2_concat3(z: Z<T>?) = "-" + z + z

fun box(): String {
    assertEquals("Z(value=42)", test1_1(Z(42)))
    assertEquals("Z(value=42)Z(value=42)", test1_2(Z(42)))
    assertEquals("Z(value=42) Z(value=42) Z(value=42)", test1_many(Z(42)))
    assertEquals("-Z(value=42)", test1_concat1(Z(42)))
    assertEquals("Z(value=42)Z(value=42)", test1_concat2(Z(42)))
    assertEquals("-Z(value=42)Z(value=42)", test1_concat3(Z(42)))

    assertEquals("null", test2_1<Int>(null))
    assertEquals("nullnull", test2_2<Int>(null))
    assertEquals("null null null", test2_many<Int>(null))
    assertEquals("-null", test2_concat1<Int>(null))
    assertEquals("nullnull", test2_concat2<Int>(null))
    assertEquals("-nullnull", test2_concat3<Int>(null))

    assertEquals("Z(value=42)", test2_1(Z(42)))
    assertEquals("Z(value=42)Z(value=42)", test2_2(Z(42)))
    assertEquals("Z(value=42) Z(value=42) Z(value=42)", test2_many(Z(42)))
    assertEquals("-Z(value=42)", test2_concat1(Z(42)))
    assertEquals("Z(value=42)Z(value=42)", test2_concat2(Z(42)))
    assertEquals("-Z(value=42)Z(value=42)", test2_concat3(Z(42)))

    return "OK"
}