// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 invoke

fun box(): String {
    var ok = "FAILED"
    { s: String ->
        ok = s
    }("OK")
    return ok
}
