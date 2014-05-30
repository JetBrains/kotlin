// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 1

    <selection>b += a
    c -= a
    println(b)
    println(c)</selection>

    return b
}