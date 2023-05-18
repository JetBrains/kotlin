// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// IGNORING_WASM_FOR_K2
// IGNORE_BACKEND: WASM
// FIR status: Disabling of StrictOnlyInputTypesChecks is not supported by FIR
// WITH_STDLIB
// !LANGUAGE: -StrictOnlyInputTypesChecks

fun foo(fn: () -> Boolean) {}

fun box(): String {
    foo { 1 in setOf("1") }
    val a = 1 in setOf("1")
    return "OK"
}
