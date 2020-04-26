// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val m = hashMapOf<String, String?>()
    m.put("b", null)
    val oldValue = m.getOrPut("b", { "Foo" })
    return if (oldValue == "Foo") "OK" else "fail: $oldValue"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_MAP
