// WITH_RUNTIME

fun box(): String {
    val a = 1
    val x: Any = Pair(10, 2)
    when (x) {
        match Pair<*, *>(a, #(a + 1)) -> return "OK"
    }
    return "fail : x must be matched"
}