// WITH_STDLIB

fun test(a: Int): Int {
    val b: Int = a + 10
    for (n in 1..b) {
        <expr>if (n > 5) throw Exception("")
        if (a + n > b) break
        println(b - n)</expr>
    }
}