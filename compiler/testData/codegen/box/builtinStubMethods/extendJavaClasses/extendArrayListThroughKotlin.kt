// WITH_STDLIB
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// DONT_TARGET_EXACT_BACKEND: NATIVE

open class A : ArrayList<String>()

class B : A()

fun box(): String {
    val b = B()
    b += "OK"
    return b.single()
}
