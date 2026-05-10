fun test() {
    var a: Any? = null

    do {
        consume(a)
    } while (a == null)

    <expr>a</expr>
}

fun consume(obj: Any?) {}