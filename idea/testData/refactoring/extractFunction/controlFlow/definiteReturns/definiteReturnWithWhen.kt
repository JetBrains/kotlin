// NEXT_SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    <selection>when (a + b) {
        0 -> return b
        1 -> return -b
        else -> return a - b
    }</selection>
}