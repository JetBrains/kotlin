// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>if (a > 0) {
        b = b + 1
    }
    else if (a < 0) {
        b--
    }
    else {
        b = a
    }
    println(b)</selection>

    return b
}