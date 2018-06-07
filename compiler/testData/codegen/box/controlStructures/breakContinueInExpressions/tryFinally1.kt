// IGNORE_BACKEND: JS_IR
fun box(): String {
    var x = "OK"
    while (true) {
        try {
            x = x + continue
        }
        finally {
            x = x + break
        }
    }
    return x
}
