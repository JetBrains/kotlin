package test

import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val facadeJClass = Class.forName("test.TestPackage") as Class<Any>

    assertEquals(facadeJClass, facadeJClass.kotlinPackage.javaFacade)
    assertEquals(facadeJClass, facadeJClass.kotlin.java)

    return "OK"
}
