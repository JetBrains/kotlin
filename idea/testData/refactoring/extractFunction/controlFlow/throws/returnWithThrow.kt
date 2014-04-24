// NEXT_SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1

    <selection>if (a > 0) throw Exception("") else return a + b
    return a - b</selection>
}