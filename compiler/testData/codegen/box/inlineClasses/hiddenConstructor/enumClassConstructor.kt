// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

inline class S(val string: String)

enum class Test(val s: S) {
    OK(S("OK"))
}

fun box() = Test.OK.s.string
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
