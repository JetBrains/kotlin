class A {
    fun bar(x: String): Int {}
}

fun foo(x: (A, String) -> Int) {}

fun main() {
    foo(A::bar)
}
