fun interface I {
    fun action(a: String, a1: Int, a2: Long)
}

fun foo(l: Long) {}

fun test() {
    <caret>I { _, _, a -> foo(a) }
}