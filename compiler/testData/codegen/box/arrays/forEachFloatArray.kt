// IGNORE_BACKEND: JS_IR
fun box(): String {
    for (x in FloatArray(5)) {
        if (x != 0.toFloat()) return "Fail $x"
    }
    return "OK"
}
