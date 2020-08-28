// TARGET_BACKEND: JVM

// KT-11447 Multifile declaration causes IAE: Method can not access a member of class
// WITH_REFLECT
// FILE: Test1.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

import kotlin.test.assertEquals

var x = 1

fun box(): String {
    assertEquals("x", ::x.name)
    assertEquals("y", ::y.name)
    assertEquals("MAGIC_NUMBER", ::MAGIC_NUMBER.name)

    assertEquals(1, ::x.call())
    assertEquals(1, ::x.getter.call())

    assertEquals(239, ::y.call())
    assertEquals(239, ::y.getter.call())

    assertEquals(42, ::MAGIC_NUMBER.call())
    assertEquals(42, ::MAGIC_NUMBER.getter.call())

    assertEquals(Unit, ::x.setter.call(2))
    assertEquals(2, ::x.call())
    assertEquals(2, ::x.getter.call())

    return "OK"
}

// FILE: Test2.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

val y = 239

const val MAGIC_NUMBER = 42
