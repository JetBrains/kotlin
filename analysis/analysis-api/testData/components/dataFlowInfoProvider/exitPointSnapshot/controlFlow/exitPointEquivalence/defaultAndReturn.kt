fun foo(a: Int) {
    val b: Int = 1

    <expr>if (a > 0) throw Exception("")
    if (b + a > 0) return
    consume(a - b)</expr>
}

fun consume(obj: Any?) {}