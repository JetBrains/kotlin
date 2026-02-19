fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <expr>if (a + b > 0) break
        else {
            consume(a - b)
            if (a - b > 0) break else consume(a + b)
        }
        </expr>
    }
    return 1
}

fun consume(obj: Any?) {}