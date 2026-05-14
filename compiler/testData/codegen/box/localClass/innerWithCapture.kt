// WITH_STDLIB

import kotlin.test.*

fun foo(s: String): String {
    class Local {
        open inner class Inner() {
            open fun result() = s
        }
    }

    return Local().Inner().result()
}

fun box(): String {
    return foo("OK")
}
