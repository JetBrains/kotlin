data class D(val x: Int, val y: Int, val z: Int)
fun foo(): Int {
    val (x, y, z) = D(1, 2, 3)
    return y + z // x is not used, but we cannot do anything with it
}
fun bar(): Int {
    val (x, y, <!UNUSED_VARIABLE!>z<!>) = D(1, 2, 3)
    return y + x // z is not used
}
