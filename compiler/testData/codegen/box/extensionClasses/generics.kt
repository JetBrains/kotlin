// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

context(T) class B<T : CharSequence> {
    val result = if (length == 2) "OK" else "fail"
}

fun box() = with("OK") {
    B().result
}
