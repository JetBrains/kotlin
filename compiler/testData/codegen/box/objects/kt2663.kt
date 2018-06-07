// IGNORE_BACKEND: JS_IR
fun box() : String {
    var a = 1

    object {
        init {
            a = 2
        }
    }
    return if (a == 2) "OK" else "fail"
}
