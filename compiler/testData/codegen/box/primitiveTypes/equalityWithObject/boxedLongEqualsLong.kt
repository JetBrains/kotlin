val x: Long = 0L

fun box(): String {
    val ax: Long? = 0L
    return if (ax != x) "Fail" else "OK"
}