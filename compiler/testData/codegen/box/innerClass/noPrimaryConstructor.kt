// WITH_STDLIB

import kotlin.test.*

class Outer(val s: String) {
    inner class Inner {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun foo() = s
    }

}

fun box(): String {
    assertEquals("OK", Outer("OK").Inner(42).foo())
    return Outer("OK").Inner("zzz").foo()
}
