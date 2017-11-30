// WITH_RUNTIME

fun box(): String {
    val x: Any = Pair(10, 2)
    when (x) {
        match Pair<*, *>(a, b) -> return "OK"
        match a -> return "FAIL"
    }
    return "FAIL"
}