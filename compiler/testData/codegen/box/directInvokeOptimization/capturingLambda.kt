// CHECK_BYTECODE_TEXT
// 0 invoke

fun box(): String {
    var ok = "FAILED"
    { s: String ->
        ok = s
    }("OK")
    return ok
}
