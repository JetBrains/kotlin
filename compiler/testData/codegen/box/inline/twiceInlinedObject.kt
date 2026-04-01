// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()

inline fun exec(f: () -> Unit) = f()

inline fun test2() {
    val obj = object {
        fun sayOk() = sb.append("OK")
    }
    obj.sayOk()
}

inline fun noExec(f: () -> Unit) { }

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    exec {
        test2()
    }
    noExec {
        test2()
    }

    return sb.toString()
}
