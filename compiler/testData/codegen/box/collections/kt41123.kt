// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: NATIVE
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// WITH_STDLIB

open class A : HashMap<String, String>()

fun box(): String {
    val a = object : A() {}
    a["OK"] = "OK"
    return a["OK"]!!
}