class A(val n: Int) {
    fun <caret>foo(): Boolean = n > 1
}

fun test() {
    val t = A(1).foo()
}