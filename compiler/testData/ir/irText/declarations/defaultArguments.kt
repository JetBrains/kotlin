// FIR_IDENTICAL
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^KT-57434

fun test1(x: Int, y: Int = 0, z: String = "abc") {
    fun local(xx: Int = x, yy: Int = y, zz: String = z) {}
}
