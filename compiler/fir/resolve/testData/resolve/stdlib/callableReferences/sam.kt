interface MySam {
    fun run(x: String): Int
}

class A {
    fun bar(x: String): Int {}
}

fun foo(x: MySam) {}

fun main() {
    val a = A()
    foo(a::bar)
}
