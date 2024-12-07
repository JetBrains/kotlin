// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// MODULE: lib
// FILE: lib.kt
package lib

import kotlin.reflect.*

inline fun <reified T> f() {
    val type: KType? = typeOf<T>()
    if (type == null) throw IllegalStateException()
}

// MODULE: box(lib)
// FILE: box.kt
import lib.f

fun box(): String {
    f<String>()
    return "OK"
}
