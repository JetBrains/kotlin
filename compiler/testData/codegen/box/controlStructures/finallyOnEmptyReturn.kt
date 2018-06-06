// IGNORE_BACKEND: JS_IR
var result = "Fail"

fun foo() {
    try {
        return
    } finally {
        result = "OK"
    }
}

fun box(): String {
    foo()
    return result
}
