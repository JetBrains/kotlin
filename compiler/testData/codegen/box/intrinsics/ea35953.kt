// IGNORE_BACKEND: JS_IR
fun box(): String {
    if (12.toString().equals("13")) {
        return "Fail"
    }
    return "OK"
}
