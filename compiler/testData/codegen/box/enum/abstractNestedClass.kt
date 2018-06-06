// IGNORE_BACKEND: JS_IR
enum class E {
    ENTRY;

    abstract class Nested
}

fun box(): String {
    E.ENTRY
    return "OK"
}
