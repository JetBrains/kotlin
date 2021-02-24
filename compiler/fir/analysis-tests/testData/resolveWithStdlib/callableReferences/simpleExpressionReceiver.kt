class A {
    fun bar(x: String): Int {}
}

fun foo(x: (String) -> Int) {}

fun main() {
    val a = A()
    foo(a::bar)
}
