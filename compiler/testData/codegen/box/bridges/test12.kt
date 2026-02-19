// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

abstract class A<in T> {
    abstract fun foo(x: T)
}

class B : A<Int>() {
    override fun foo(x: Int) {
        sb.appendLine("B: $x")
    }
}

class C : A<Any>() {
    override fun foo(x: Any) {
        sb.appendLine("C: $x")
    }
}

fun foo(arg: A<Int>) {
    arg.foo(42)
}

fun box(): String {
    foo(B())
    foo(C())

    assertEquals("B: 42\nC: 42\n", sb.toString())
    return "OK"
}
