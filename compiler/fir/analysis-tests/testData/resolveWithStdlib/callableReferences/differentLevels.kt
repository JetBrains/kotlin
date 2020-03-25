fun foo(x: () -> Int, y: Int) {}
fun bar(x: String): Int = 1

fun main() {
    fun bar(): Int = 1
    fun foo(x: (String) -> Int, y: String) {}

    foo(::bar, 1)
    foo(::bar, "")
}
