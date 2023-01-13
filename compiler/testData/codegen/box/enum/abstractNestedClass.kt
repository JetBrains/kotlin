// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class E {
    ENTRY;

    abstract class Nested
}

fun box(): String {
    E.ENTRY
    return "OK"
}
