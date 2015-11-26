// WITH_RUNTIME
// IS_APPLICABLE: false
class A(val n: Int) {
    operator fun <caret>invoke(): Int = 1
}

fun test(a: A) {
    val n = a()
}