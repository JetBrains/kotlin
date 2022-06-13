// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 invoke

class Outer (val x: String) {
    inner class Inner(val y: String) {
        val yx = y + x
    }
}

fun box() =
    Outer("K")::Inner.invoke("O").yx
