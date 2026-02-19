// WITH_STDLIB

import kotlin.test.*

fun foo() {
    val cls1: Any? = Int
    val cls2: Any? = null

    cls1?.let {
        cls2?.let {
            var itClass = it::class
        }
    }
}

fun box(): String {
    foo()
    return "OK"
}
