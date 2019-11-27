// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun contains(set: Set<Any>, x: Int): Boolean = when {
    set.size == 0 -> false
    else -> x in set as Set<Int>
}

fun box(): String {
    val set = setOf(1)
    if (contains(set, 1)) {
        return "OK"
    }

    return "Fail"
}
