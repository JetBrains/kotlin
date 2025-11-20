// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

inline fun exec(f: () -> Unit) = f()

inline fun test2() {
    val obj = object {
        fun sayOk() = sb.append("OK")
    }
    obj.sayOk()
}

inline fun noExec(f: () -> Unit) { }

fun box(): String {
    exec {
        test2()
    }
    noExec {
        test2()
    }

    return sb.toString()
}
