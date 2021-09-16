// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

context(Collection<P>) class A<P> {
    val result = if (!isEmpty()) "OK" else "fail"
}

fun box(): String {
    with (listOf(1, 2, 3)) {
        return A().result
    }
}