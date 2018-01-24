// WITH_RUNTIME

fun box(): String {
    val x: Pair<Int, Int>? = null;
    when (x) {
        is Pair(1, 2) -> return "FAIL"
    }
    return "OK"
}
