// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun Any?.foo() {}

fun test(a: Any?) {
    if (a != null) {
        a.foo()
    }
}