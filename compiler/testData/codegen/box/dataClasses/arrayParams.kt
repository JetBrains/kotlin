// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Array<Int>, val y: IntArray)

fun foo(x: Array<Int>, y: IntArray) = A(x, y)

fun box(): String {
    val a = Array<Int>(0, {0})
    val b = IntArray(0)
    val (x, y) = foo(a, b)
    return if (a == x && b == y) "OK" else "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
