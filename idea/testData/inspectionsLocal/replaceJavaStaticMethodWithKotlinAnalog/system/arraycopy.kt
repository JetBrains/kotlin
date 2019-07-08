// WITH_RUNTIME
fun test() {
    val a = arrayOf(2)
    val b = arrayOf(4)
    val aOffset = 1
    val bOffset = 2
    val len = 5
    java.lang.System.<caret>arraycopy(a, aOffset, b, bOffset, len)
}