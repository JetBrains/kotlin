// SHOULD_FAIL_WITH: Function foo already exists
class A(val n: Int) {
    val <caret>foo: Boolean = n > 1
}

fun A.foo() = 1