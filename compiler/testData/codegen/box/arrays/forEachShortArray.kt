// IGNORE_BACKEND: JS_IR
fun box(): String {
    for (x in ShortArray(5)) {
        if (x != 0.toShort()) return "Fail $x"
    }
    return "OK"
}
