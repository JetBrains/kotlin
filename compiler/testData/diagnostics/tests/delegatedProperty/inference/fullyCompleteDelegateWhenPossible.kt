// FIR_IDENTICAL
// WITH_STDLIB

import kotlin.reflect.KProperty

class Cached<T>(val f: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = TODO()
}

private val map: MutableMap<String, Cached<String>> = TODO()

fun foo() {
    val bar by map.getOrPut("A") {
        Cached { "B" }
    }
}
