// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestKt")
package test

import kotlin.test.assertEquals

private var prop = "O"

private fun test() = "K"

fun box(): String {

    val clazz = Class.forName("test.TestKt")
    assertEquals(1, clazz.declaredMethods.size, "Facade should have only box and getProp methods")
    assertEquals("box", clazz.declaredMethods.first().name, "Facade should have only box method")

    return {
        prop + test()
    }()
}
