// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt

val sb = StringBuilder()

inline fun <R> call(block: ()->R): R {
    try {
        return block()
    } finally {
        sb.append("OK")
    }
}

// FILE: main.kt
import kotlin.test.*
fun box(): String {
    call { class Z(); Z() }
    return sb.toString()
}
