// WITH_STDLIB

import kotlin.test.*

fun foo(s: String): String {
    class Local {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun result() = s
    }

    return Local(42).result() + Local("zzz").result()
}

fun box(): String {
    assertEquals("OKOK", foo("OK"))
    return "OK"
}
