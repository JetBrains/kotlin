// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testArgumentInCall() {
    fun bar(i: Int, s: String, x: Any) {}

    bar(1, todo(), "")
}

fun testArgumentInVariableAsFunctionCall(f: (Any) -> Unit) {
    f(todo())
}

fun todo(): Nothing = throw Exception()