// IGNORE_BACKEND: JS_IR
fun foo(args: Array<String>) {
    try {
    } finally {
        try {
        } catch (e: Throwable) {
        }
    }
}

fun box() = "OK"
