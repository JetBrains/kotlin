// WITH_STDLIB
fun foo(): Int {
    fun action(s: String): Int = s.toInt()

    val <!UNUSED_VARIABLE!>localAction<!> = ::action

    return localAction("hello")
}