// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return when (<selection>a + b</selection>) {
        0 -> b
        1 -> -b
        else -> a - b
    }
}
