// IGNORE_BACKEND: JS_IR
fun box(): String {
    (0.toLong() as Number?)?.toByte()
    (0 as Int?)?.toDouble()
    return "OK"
}
