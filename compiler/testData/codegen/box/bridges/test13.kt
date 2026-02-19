// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

open class A<T> {
    open fun T.foo() {
        sb.appendLine(this.toString())
    }

    fun bar(x: T) {
        x.foo()
    }
}

open class B: A<Int>() {
    override fun Int.foo() {
        sb.appendLine(this.toString())
    }
}

fun box(): String {
    val b = B()
    val a = A<Int>()
    b.bar(42)
    a.bar(42)

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}
