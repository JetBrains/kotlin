// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: 1.kt

@file:JvmMultifileClass
@file:JvmName("Test")

package test

import kotlin.reflect.*

object Delegate {
    lateinit var property: KProperty<*>

    operator fun getValue(instance: Any?, kProperty: KProperty<*>): List<String?> {
        property = kProperty
        return emptyList()
    }
}

// FILE: 2.kt

@file:JvmMultifileClass
@file:JvmName("Test")

package test

fun foo() {
    val x by Delegate
    x
}

// FILE: test.kt

import test.*
import kotlin.test.assertEquals

fun box(): String {
    foo()
    assertEquals("val x: kotlin.collections.List<kotlin.String?>", Delegate.property.toString())
    return "OK"
}
