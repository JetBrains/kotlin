// FLOW: OUT

fun String.foo(<caret>p: String) {
    val v = f({ p1, p2 -> p2 }, p)
}

fun f(lambda: (String, String) -> String, receiver: String): String {
    return lambda("a", receiver)
}
