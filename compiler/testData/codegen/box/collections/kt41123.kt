// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: NATIVE
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-57268

open class A : HashMap<String, String>()

fun box(): String {
    val a = object : A() {}
    a["OK"] = "OK"
    return a["OK"]!!
}