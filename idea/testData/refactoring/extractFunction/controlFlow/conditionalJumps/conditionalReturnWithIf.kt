// NEXT_SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    <selection>if (a + b > 0) return 0
    println(a - b)</selection>
    return 1
}