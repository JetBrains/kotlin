// SIBLING:
fun foo(a: Int) {
    val b: Int = 1
    @loop1 for (p in 1..b) {
        @loop2 for (n in 1..b) {
            <selection>if (a > 0) throw Exception("")
            if (a + b > 0) break@loop1
            println(a - b)
            break@loop2</selection>
        }
    }
}