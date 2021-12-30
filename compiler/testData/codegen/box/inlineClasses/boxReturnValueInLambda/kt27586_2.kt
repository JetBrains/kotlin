// WITH_STDLIB

fun f1() = lazy {
    runCatching {
        "OK"
    }
}

fun box(): String {
    val r = f1().value
    return r.getOrNull() ?: "fail: $r"
}