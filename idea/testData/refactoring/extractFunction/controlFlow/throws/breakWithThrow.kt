// NEXT_SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..b) {
        <selection>if (a > 0) throw Exception("")
        if (a + b > 0) break
        println(a - b)</selection>
    }
}