// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

package test

import kotlin.test.assertEquals

class Klass {
    class Nested
    companion object
}

class AnotherKlass {
    object Nested
    companion object Default
}

object TopLevelObject

interface MyInterface

enum class MyEnum { ENTRY }

annotation class MyAnnotation

class Generic<T> {
    inner class Inner
}

fun box(): String {
    assertEquals("test.Klass", Klass::class.qualifiedName)
    assertEquals("test.Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("test.Klass.Companion", Klass.Companion::class.qualifiedName)

    assertEquals("test.AnotherKlass", AnotherKlass::class.qualifiedName)
    assertEquals("test.AnotherKlass.Nested", AnotherKlass.Nested::class.qualifiedName)
    assertEquals("test.AnotherKlass.Default", AnotherKlass.Default::class.qualifiedName)

    assertEquals("test.TopLevelObject", TopLevelObject::class.qualifiedName)


    assertEquals("test.MyInterface", MyInterface::class.qualifiedName)
    assertEquals("test.MyEnum", MyEnum::class.qualifiedName)
    assertEquals("test.MyEnum", MyEnum.ENTRY::class.qualifiedName)
    assertEquals("test.MyAnnotation", MyAnnotation::class.qualifiedName)
    assertEquals("test.Generic", Generic::class.qualifiedName)
    assertEquals("test.Generic.Inner", Generic.Inner::class.qualifiedName)

    val inner = Generic<Int>().Inner()
    assertEquals("test.Generic.Inner", inner::class.qualifiedName)
    class Local
    assertEquals(null, Local::class.qualifiedName)
    val anonimous = object {}
    assertEquals(null, anonimous::class.qualifiedName)

    return "OK"
}
