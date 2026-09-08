fun test() {
    var a: Any? = null

    while (true) {
        if (a == null) return
        if (foo()) break
    }

    <expr>a</expr>
}

fun foo(): Boolean = true
