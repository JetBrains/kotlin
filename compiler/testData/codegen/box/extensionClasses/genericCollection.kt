// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// WITH_STDLIB

context(Collection<P>) class A<P> {
    val result = if (!isEmpty()) "OK" else "fail"
}

fun box(): String {
    with (listOf(1, 2, 3)) {
        return A().result
    }
}
