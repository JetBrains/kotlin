fun foo(a: Int) {
    val b: Int = 1
    loop1@ for (p in 1..b) {
        loop2@ for (n in 1..b) {
            <expr>if (a > 0) throw Exception("")
            if (a + b > 0) break@loop1
            consume(a - b)
            break@loop2</expr>
        }
    }
}

fun consume(obj: Any?) {}