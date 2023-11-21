// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

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
