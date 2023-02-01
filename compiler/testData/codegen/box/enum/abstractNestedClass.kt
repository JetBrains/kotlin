enum class E {
    ENTRY;

    abstract class Nested
}

fun box(): String {
    E.ENTRY
    return "OK"
}
