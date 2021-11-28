// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestKt")
package test

import kotlin.test.assertEquals
import kotlin.test.assertTrue

public var prop = "fail"
    private set

private fun test() = "K"

fun box(): String {

    val clazz = Class.forName("test.TestKt")
    assertEquals(2, clazz.declaredMethods.size, "Facade should have only box method")
    val methods = clazz.declaredMethods.map { it.name }
    assertTrue(methods.contains("box"), "Facade should have box method")
    assertTrue(methods.contains("getProp"), "Facade should have box method")

    return {
        prop = "O"
        prop + test()
    }()
}
