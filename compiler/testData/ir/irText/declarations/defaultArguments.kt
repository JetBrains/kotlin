// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57434

fun test1(x: Int, y: Int = 0, z: String = "abc") {
    fun local(xx: Int = x, yy: Int = y, zz: String = z) {}
}
