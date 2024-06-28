class Foo {
    companion object
}

fun test() {
    consume(F<caret>oo)
}

private fun consume(obj: Any) {}