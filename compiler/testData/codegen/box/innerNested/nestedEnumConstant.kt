// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    enum class Nested {
        O,
        K
    }
}

fun box() = "${Outer.Nested.O}${Outer.Nested.K}"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
