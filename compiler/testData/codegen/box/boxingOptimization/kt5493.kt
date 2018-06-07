// IGNORE_BACKEND: JS_IR
fun box() : String {
    try {
        return "OK"
    }
    finally {
        null?.toString()
    }
}
