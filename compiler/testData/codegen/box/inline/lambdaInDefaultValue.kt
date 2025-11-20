// WITH_STDLIB

import kotlin.test.*

inline fun inlineFun(param: String, lambda: (String) -> String = { it }): String {
    return lambda(param)
}

fun box(): String {
    return inlineFun("OK")
}
