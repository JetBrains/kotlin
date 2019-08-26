// IGNORE_BACKEND: WASM
fun <T: Number?> foo(t: T) {
    (t ?: 42).toInt()
}

fun box(): String {
    foo<Int?>(null)
    return "OK"
}
