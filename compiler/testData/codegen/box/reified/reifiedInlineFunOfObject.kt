// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

interface A {
    fun f(): String
    fun g(): String
}

fun box(): String {
    val x: A = object : A {
        private inline fun <reified T : Any> localClassName(): String = T::class.java.getName()
        override fun f(): String = foo { localClassName<String>() }
        override fun g(): String = foo { localClassName<Int>() }
    }

    assertEquals("java.lang.String", x.f())
    assertEquals("java.lang.Integer", x.g())

    return "OK"
}
