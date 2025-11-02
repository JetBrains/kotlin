// This being legal feels wrong, but if it is a valid construction, it should compile and run properly
enum class E {
    A, B, C
}

fun foo(e: E): String {
    return when {
        false -> "Fail"
        else -> "OK"
    }
}

fun box(): String {
    return foo(E.A)
}