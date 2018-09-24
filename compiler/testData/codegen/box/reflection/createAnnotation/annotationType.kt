// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

annotation class Foo

fun box(): String {
    val foo = Foo::class.constructors.single().call()
    assertEquals(Foo::class, foo.annotationClass)
    return "OK"
}
