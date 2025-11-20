// WITH_STDLIB

import kotlin.test.*

fun foo() {
    val cls1: Any? = Int
    val cls2: Any? = null

    cls1?.let {
        if (cls2 != null) {
            val zzz = 42
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}
