// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 Function(^.)*.invoke

fun box() =
    (fun (s: String): String {
        var ok = "O"
        ok += s
        return ok
    }).invoke("K")
