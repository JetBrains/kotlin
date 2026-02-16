fun test() {
    var a: Any? = null

    do {
        if (a == null) return
    } while (foo())

    <expr>a</expr>
}

fun foo(): Boolean = true