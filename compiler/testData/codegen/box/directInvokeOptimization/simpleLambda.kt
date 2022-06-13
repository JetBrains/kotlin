// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 invoke

fun box() =
    { s: String ->
        var ok = "O"
        ok += s
        ok
    }.invoke("K")
