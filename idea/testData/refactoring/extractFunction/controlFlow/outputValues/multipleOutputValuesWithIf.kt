// NEXT_SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 1

    <selection>if (a > 0) {
        b += a
    }
    else {
        c -= a
    }
    println(b)
    println(c)</selection>

    return b
}