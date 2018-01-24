// WITH_RUNTIME

fun box(): String {
    val x: Any = Pair(10, 2)
    when (x) {
        is Pair(val a, val b) -> return "OK"
        is val a -> return "FAIL"
    }
    return "FAIL"
}