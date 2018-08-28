// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS

class `A!u00A0`() {
    val ok = "OK"
}

fun box(): String {
    return `A!u00A0`().ok
}