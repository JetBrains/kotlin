// IGNORE_BACKEND: JVM_IR
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
