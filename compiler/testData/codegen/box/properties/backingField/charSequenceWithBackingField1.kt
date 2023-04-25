// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

class Base {
    val x: CharSequence
        internal field: String = "OK"
}

fun box(): String {
    return Base().x
}
