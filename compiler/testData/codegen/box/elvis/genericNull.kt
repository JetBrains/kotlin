// IGNORE_BACKEND: JS_IR
fun <T: Number?> foo(t: T) {
    (t ?: 42).toInt()
}

fun box(): String {
    foo<Int?>(null)
    return "OK"
}
