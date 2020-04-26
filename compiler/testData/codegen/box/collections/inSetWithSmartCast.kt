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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: STDLIB_HASH_SET


