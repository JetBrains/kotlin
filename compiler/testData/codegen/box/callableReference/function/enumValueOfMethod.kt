// IGNORE_BACKEND: JS_IR
enum class E {
    ENTRY
}

fun box(): String {
    val f = E::valueOf
    val result = f("ENTRY")
    return if (result == E.ENTRY) "OK" else "Fail $result"
}
