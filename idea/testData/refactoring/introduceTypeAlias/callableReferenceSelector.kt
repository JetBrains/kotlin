// NAME: T
class A {
    fun bar() = 1
}

// SIBLING:
fun foo() {
    val a = A::<caret>bar
}