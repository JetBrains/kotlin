// NEXT_SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    val t = <selection>if (a > 0) {
        b += a
        b
    }
    else {
        a
    }</selection>
    println(b)

    return t
}