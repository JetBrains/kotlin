fun <T> foo(x: () -> T, y: Int) {}
fun <E> bar(x: E): Int = 1

fun main() {
    fun bar(): Int = 1
    fun foo(x: (String) -> Int, y: String) {}

    foo(::bar, 1)
    foo(::bar, "")
}
