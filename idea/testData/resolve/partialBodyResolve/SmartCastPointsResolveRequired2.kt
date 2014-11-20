fun foo(x: Any?) {
    if (x == null) {
        val error = f()
        error("error")
    }

    <caret>x.hashCode()
}

fun f(): (String) -> Unit = {}
