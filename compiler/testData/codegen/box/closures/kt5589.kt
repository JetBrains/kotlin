fun box(): String {
    val x = "OK"
    fun bar(y: String = x): String = y
    return bar()
}