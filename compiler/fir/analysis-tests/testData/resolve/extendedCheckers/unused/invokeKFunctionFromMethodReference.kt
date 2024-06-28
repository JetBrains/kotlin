// WITH_STDLIB
fun foo(): Int {
    fun action(s: String): Int = s.toInt()

    val localAction = ::action

    return localAction("hello")
}