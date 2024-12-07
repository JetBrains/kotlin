// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported
// WITH_STDLIB

context(Collection<P>) class A<P> {
    val result = if (!isEmpty()) "OK" else "fail"
}

fun box(): String {
    with (listOf(1, 2, 3)) {
        return A().result
    }
}
