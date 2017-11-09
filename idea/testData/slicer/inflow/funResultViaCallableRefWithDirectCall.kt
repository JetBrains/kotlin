// FLOW: IN

fun test() {
    fun bar(n: Int) = n
    val <caret>x = (::bar)(1)
}