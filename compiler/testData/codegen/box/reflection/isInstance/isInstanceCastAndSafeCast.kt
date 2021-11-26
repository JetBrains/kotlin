// !OPT_IN: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.safeCast
import kotlin.test.*

fun testInstance(value: Any?, klass: KClass<*>) {
    assertTrue(klass.isInstance(value))
    assertEquals(value, klass.safeCast(value))
    assertEquals(value, klass.cast(value))
}

fun testNotInstance(value: Any?, klass: KClass<*>) {
    assertFalse(klass.isInstance(value))
    assertNull(klass.safeCast(value))
    try {
        klass.cast(value)
        fail("Value should not be an instance of $klass: $value")
    }
    catch (e: Exception) { /* OK */ }
}

fun box(): String {
    testInstance(Any(), Any::class)
    testInstance("", String::class)
    testInstance("", Any::class)
    testNotInstance(Any(), String::class)
    testNotInstance(null, Any::class)
    testNotInstance(null, String::class)

    testInstance(arrayOf(""), Array<String>::class)
    testInstance(arrayOf(""), Array<Any>::class)
    testNotInstance(arrayOf(Any()), Array<String>::class)

    testInstance(listOf(""), List::class)
    testInstance(listOf(""), Collection::class)
    // TODO: support MutableList::class (KT-11754)
    // testNotInstance(listOf(""), MutableList::class)

    testInstance(42, Int::class)
    testInstance(42, Int::class.javaPrimitiveType!!.kotlin)
    testInstance(42, Int::class.javaObjectType!!.kotlin)

    testNotInstance(3.14, Int::class)

    // Function types

    testInstance(fun() {}, Function0::class)
    testNotInstance(fun() {}, Function1::class)
    testNotInstance(fun() {}, Function2::class)

    testNotInstance(::testInstance, Function0::class)
    testNotInstance(::testInstance, Function1::class)
    testInstance(::testInstance, Function2::class)

    return "OK"
}
