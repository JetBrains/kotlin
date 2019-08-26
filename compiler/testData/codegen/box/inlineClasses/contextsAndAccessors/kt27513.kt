// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class A(val b: String) {
    override fun toString(): String =
        buildString { append(b) }
}

fun box() = A("OK").toString()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ buildString 
