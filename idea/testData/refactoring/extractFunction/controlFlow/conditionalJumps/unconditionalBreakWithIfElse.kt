// NEXT_SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <selection>if (a + b > 0) break
        else {
            println(a - b)
            break
        }</selection>
    }
    return 1
}