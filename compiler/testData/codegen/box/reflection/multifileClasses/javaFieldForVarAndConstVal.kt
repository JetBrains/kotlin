// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK
// FILE: 1.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun testX() {
    val field = ::x.javaField ?: throw AssertionError("No java field for ${::x.name}")

    try {
        field.get(null)
        throw AssertionError("Fail: field.get should fail because the field is private")
    }
    catch (e: IllegalAccessException) {
        // OK
    }

    field.setAccessible(true)
    assertEquals("I am x", field.get(null))
    field.set(null, "OK")
}

fun testY() {
    val field = ::y.javaField ?: throw AssertionError("No java field for ${::y.name}")

    assertEquals("I am const y", field.get(null))

    // Accessible = false should have no effect because the field is public
    field.setAccessible(false)

    assertEquals("I am const y", field.get(null))
}

fun testZ() {
    val field = refZ.javaField ?: throw AssertionError("No java field for ${refZ.name}")


    try {
        field.get(null)
        throw AssertionError("IllegalAccessError expected")
    }
    catch (e: IllegalAccessException) {
        // OK
    }

    field.setAccessible(true)
    assertEquals("I am private const val Z", field.get(null))
}

fun box(): String {
    testX()
    testY()
    testZ()
    return x
}

// FILE: 2.kt

@file:kotlin.jvm.JvmName("Test")
@file:kotlin.jvm.JvmMultifileClass
package test

var x = "I am x"
const val y = "I am const y"
private const val z = "I am private const val Z"

val refZ = ::z