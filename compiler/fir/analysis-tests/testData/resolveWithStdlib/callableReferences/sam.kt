fun interface MySam {
    fun run(x: String): Int
}

class A {
    fun bar(x: String): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

fun foo(x: MySam) {}

fun main() {
    val a = A()
    foo(a::bar)
}
