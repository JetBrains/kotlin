// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

inline fun <R> call(block: ()->R): R {
    try {
        return block()
    } finally {
        sb.append("OK")
    }
}

fun box(): String {
    call { class Z(); Z() }
    return sb.toString()
}
