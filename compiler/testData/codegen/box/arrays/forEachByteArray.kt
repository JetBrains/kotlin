// IGNORE_BACKEND: JS_IR
fun box(): String {
    for (x in ByteArray(5)) {
        if (x != 0.toByte()) return "Fail $x"
    }
    return "OK"
}
