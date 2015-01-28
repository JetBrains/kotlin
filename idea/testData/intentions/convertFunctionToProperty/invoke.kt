// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    fun <caret>invoke(): Int = 1
}

fun test(a: A) {
    val n = a()
}