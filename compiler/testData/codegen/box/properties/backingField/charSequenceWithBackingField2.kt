// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

class Base {
    val x: CharSequence
        internal field: String = "OK"

    val s: String get() = x
}

fun box(): String {
    return Base().s
}
