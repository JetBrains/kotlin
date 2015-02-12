// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    fun <caret>minus(): A = A(-n)
}

fun test() {
    val t = -A(1)
}