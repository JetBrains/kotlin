// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: Disabling of StrictOnlyInputTypesChecks is not supported by FIR
// WITH_STDLIB
// !LANGUAGE: -StrictOnlyInputTypesChecks

fun foo(fn: () -> Boolean) {}

fun box(): String {
    foo { 1 in setOf("1") }
    val a = 1 in setOf("1")
    return "OK"
}
