// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return if (a + b > 0) 1 else <selection>if (a - b < 0) 2 else b</selection>
}
