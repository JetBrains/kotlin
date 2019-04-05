// FIR_IDENTICAL

fun test1(x: Int, y: Int = 0, z: String = "abc") {
    fun local(xx: Int = x, yy: Int = y, zz: String = z) {}
}
