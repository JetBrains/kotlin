// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1

    <selection>if (a > 0) return a
    if (a < 0) return b
    </selection>

    return t
}