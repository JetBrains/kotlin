// WITH_STDLIB

import kotlin.test.*

fun foo(s: String): String {
    open class Local {
        fun f() = s
    }

    open class Derived: Local() {
        fun g() = f()
    }

    return Derived().g()
}

fun box(): String {
    return foo("OK")
}
