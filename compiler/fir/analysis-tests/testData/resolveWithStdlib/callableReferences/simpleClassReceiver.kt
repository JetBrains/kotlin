class A {
    fun bar(x: String): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

fun foo(x: (A, String) -> Int) {}

fun main() {
    foo(A::bar)
}
