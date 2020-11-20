// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// TARGET_BACKEND: JVM

import java.util.AbstractList

class A : AbstractList<String>() {
    override fun get(index: Int): String = ""
    override val size: Int get() = 0
}

fun box(): String {
    val a = A()
    val b = A()

    a.addAll(b)
    a.addAll(0, b)
    a.removeAll(b)
    a.retainAll(b)
    a.clear()
    a.remove("")

    return "OK"
}
