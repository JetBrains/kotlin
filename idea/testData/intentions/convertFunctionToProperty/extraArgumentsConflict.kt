// ERROR: Too many arguments for public final fun foo(): Boolean defined in A
// SHOULD_FAIL_WITH: Call with arguments will be skipped: foo(2)
class A(val n: Int) {
    fun <caret>foo(): Boolean = n > 1
}

fun test() {
    val t = A(1).foo(2)
}