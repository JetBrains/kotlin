// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

class Outer(val x: Int) {
    inner class Inner() {
        inner class InnerInner() {

            init {
                sb.appendLine(x)
            }

            lateinit var s: String

            constructor(s: String) : this() {
                this.s = s
            }
        }
    }
}

fun box(): String {
    Outer(42).Inner().InnerInner("zzz")

    assertEquals("42\n", sb.toString())
    return "OK"
}
