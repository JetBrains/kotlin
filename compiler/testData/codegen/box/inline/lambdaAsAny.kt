// WITH_STDLIB

// FILE: lib.kt
val sb = StringBuilder()

inline fun foo(x: Any) {
    sb.append(if (x === x) "OK" else "FAIL")
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    foo { 42 }

    return sb.toString()
}
