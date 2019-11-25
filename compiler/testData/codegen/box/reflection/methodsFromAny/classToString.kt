// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
package test

import kotlin.test.*

class A {
    class Nested

    companion object
}

fun box(): String {
    assertEquals("class test.A", "${A::class}")
    assertEquals("class test.A\$Nested", "${A.Nested::class}")
    assertEquals("class test.A\$Companion", "${A.Companion::class}")

    assertEquals("class kotlin.Any", "${Any::class}")
    assertEquals("class kotlin.Int", "${Int::class}")
    assertEquals("class kotlin.Int\$Companion", "${Int.Companion::class}")
    assertEquals("class kotlin.IntArray", "${IntArray::class}")
    assertEquals("class kotlin.String", "${String::class}")
    assertEquals("class kotlin.String", "${java.lang.String::class}")

    assertEquals("class kotlin.Array", "${Array<Any>::class}")
    assertEquals("class kotlin.Array", "${Array<Int>::class}")
    assertEquals("class kotlin.Array", "${Array<Array<String>>::class}")

    assertEquals("class java.lang.Runnable", "${Runnable::class}")

    return "OK"
}
