// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 Function(^.)*.invoke

fun ok(s: String) = "O" + s

fun box() =
    ::ok.invoke("K")
