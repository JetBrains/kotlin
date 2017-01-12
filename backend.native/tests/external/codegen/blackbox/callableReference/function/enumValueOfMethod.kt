// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

enum class E {
    ENTRY
}

fun box(): String {
    val f = E::valueOf
    val result = f("ENTRY")
    return if (result == E.ENTRY) "OK" else "Fail $result"
}
