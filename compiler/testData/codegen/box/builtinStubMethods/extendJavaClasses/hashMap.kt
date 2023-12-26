// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTION_INHERITANCE
// KJS_WITH_FULL_RUNTIME
// DONT_TARGET_EXACT_BACKEND: NATIVE
// JVM_ABI_K1_K2_DIFF: KT-57268

class A : HashMap<String, Double>()

fun box(): String {
    val a = A()
    val b = A()

    a.put("", 0.0)
    a.remove("")

    a.putAll(b)
    a.clear()

    a.keys
    a.values
    a.entries

    return "OK"
}
