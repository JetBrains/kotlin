// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo {
    companion object
}

fun test() {
    consume(F<caret>oo)
}

private fun consume(obj: Any) {}