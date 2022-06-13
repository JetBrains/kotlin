// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 invoke

fun box(): String =
    (String::plus)("O", "K")
