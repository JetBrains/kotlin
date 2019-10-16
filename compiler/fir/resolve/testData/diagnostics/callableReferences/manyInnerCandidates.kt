fun foo(x: (String) -> Int) {}


fun bar(y: Any): Int = 1
fun bar(x: String): Int = 1

fun main() {
    foo(::bar)
}
