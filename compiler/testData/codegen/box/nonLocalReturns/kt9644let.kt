// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun foo() {
    with(1) {
        return (1..2).forEach { it }
    }
}

fun box(): String {
    foo()
    return "OK"
}
