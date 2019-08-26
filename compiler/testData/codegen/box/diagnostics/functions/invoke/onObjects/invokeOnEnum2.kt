// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class A {
    ONE,
    TWO
}

operator fun A.invoke(i: Int) = i

fun box() = if (A.ONE(42) == 42) "OK" else "fail"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
