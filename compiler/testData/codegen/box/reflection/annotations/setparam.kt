// TARGET_BACKEND: JVM
// WITH_REFLECT

// different annotation order
// IGNORE_BACKEND: ANDROID

package test

import kotlin.test.assertEquals

annotation class Ann1
annotation class Ann2

class Foo {
    @setparam:Ann1
    var delegate = " "
        set(@Ann2 value) {}
}

fun box(): String {
    val setterParameters = Foo::delegate.setter.parameters
    assertEquals(2, setterParameters.size)
    assertEquals("[]", setterParameters.first().annotations.toString())
    assertEquals("[@test.Ann2(), @test.Ann1()]", setterParameters.last().annotations.toString())
    return "OK"
}
