// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_STRING_BUILDER
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class A(val b: String) {
    override fun toString(): String =
        buildString { append(b) }
}

fun box() = A("OK").toString()
