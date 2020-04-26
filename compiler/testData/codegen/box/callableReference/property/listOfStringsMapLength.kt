// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String =
        if (listOf("abc", "de", "f").map(String::length) == listOf(3, 2, 1)) "OK" else "Fail"



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
