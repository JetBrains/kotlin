// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// WITH_STDLIB
// DONT_TARGET_EXACT_BACKEND: NATIVE

interface A : Set<String>

class B : A, HashSet<String>()

fun box(): String {
    val b = B()
    b.add("OK")
    return b.iterator().next()
}
