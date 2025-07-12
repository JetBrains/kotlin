// TARGET_BACKEND: JVM
// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
inline fun<reified T : Any> className(): String = T::class.java.getName()

// FILE: main.kt
import kotlin.test.assertEquals

fun foo(block: () -> String) = block()

interface A {
    fun f(): String
    fun g(): String
}

fun box(): String {
    val x = foo() {
        className<String>()
    }

    assertEquals("java.lang.String", x)

    val y: A = object : A {
        override fun f(): String = foo { className<String>() }
        override fun g(): String = foo { className<Int>() }
    }

    assertEquals("java.lang.String", y.f())
    assertEquals("java.lang.Integer", y.g())

    return "OK"
}
