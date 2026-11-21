// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// ASSERTIONS_MODE: jvm

// FILE: Library.kt
inline fun wrap(crossinline f: () -> String): () -> String = { f() }

// FILE: Factory.kt
var checks = 0
fun condition(): Boolean {
    checks++
    return true
}

fun make(): () -> String = wrap {
    assert(condition())
    "OK"
}

// FILE: box.kt
class Marker

fun box(): String {
    Marker::class.java.classLoader.setDefaultAssertionStatus(true)
    val first = make()
    if (first !== make()) return "Lambda is not a singleton"
    if (first() != "OK") return "Wrong result"
    if (checks != 1) return "Assertion was not initialized or was evaluated more than once: $checks"
    return "OK"
}
