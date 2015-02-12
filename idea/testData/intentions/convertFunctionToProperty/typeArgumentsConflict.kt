// SHOULD_FAIL_WITH: Type arguments will be lost after conversion: foo&lt;Double&gt;()
class A(val n: Int) {
    fun <caret>foo<T>(): Boolean = n > 1
}

fun test() {
    val t = A(1).foo<Double>()
}