fun foo(p: String?, p1: Any?) {
    if (p1 == null) return

    fun localError(): Nothing = throw Exception()

    if (p == null) {
        localError()
    }
    <caret>p.length()
}