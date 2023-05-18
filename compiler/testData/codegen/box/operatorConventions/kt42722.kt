// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// IGNORING_WASM_FOR_K2
// IGNORE_BACKEND: WASM
// FIR status: Disabling of StrictOnlyInputTypesChecks is not supported by FIR
// WITH_STDLIB
// !LANGUAGE: -StrictOnlyInputTypesChecks

fun box(): String {
    val set = setOf<Int>(1, 2, 3, 4, 5)
    val x = 0 in set
    val y = 1 in set
    val z = null in set
    return "OK"
}
