// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    <selection>if (a + b > 0) return 0
    else if (a - b < 0) println(a - b)
    else println(0)</selection>
    return 1
}