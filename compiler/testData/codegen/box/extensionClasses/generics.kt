// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported

context(T) class B<T : CharSequence> {
    val result = if (length == 2) "OK" else "fail"
}

fun box() = with("OK") {
    B().result
}
