// CHECK_BYTECODE_TEXT
// 0 invoke\(

class Outer (val x: String) {
    inner class Inner(val y: String) {
        val yx = y + x
    }
}

fun box() =
    (Outer::Inner).invoke(Outer("K"), "O").yx
