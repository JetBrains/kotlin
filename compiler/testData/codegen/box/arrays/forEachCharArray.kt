// IGNORE_BACKEND: JS_IR
fun box(): String {
    for (x in CharArray(5)) {
        if (x != 0.toChar()) return "Fail $x"
    }
    return "OK"
}
