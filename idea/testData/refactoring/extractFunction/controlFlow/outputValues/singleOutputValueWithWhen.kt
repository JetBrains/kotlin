// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection> when {
        a > 0 -> {
            b = b + 1
        }
        a < 0 -> {
            b = b - 1
        }
    }
    println(b)</selection>

    return b
}