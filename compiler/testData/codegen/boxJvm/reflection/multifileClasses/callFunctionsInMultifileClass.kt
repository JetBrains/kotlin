// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: Test1.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

import kotlin.test.assertEquals

fun getX() = 1

fun box(): String {
    assertEquals("getX", ::getX.name)
    assertEquals("getY", ::getY.name)
    assertEquals("getZ", ::getZ.name)

    assertEquals(1, ::getX.call())
    assertEquals(239, ::getY.call())
    assertEquals(42, ::getZ.callBy(emptyMap()))

    return "OK"
}

// FILE: Test2.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

fun getY() = 239

fun getZ(value: Int = 42) = value
