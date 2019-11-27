// IGNORE_BACKEND_FIR: JVM_IR
enum class E {
    ENTRY;

    abstract class Nested
}

fun box(): String {
    E.ENTRY
    return "OK"
}
