// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 invoke

class C(x: String, y: String) {
    val yx = y + x
}

fun box() =
    ::C.invoke("K", "O").yx
