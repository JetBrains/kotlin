fun foo(x: (String) -> Int) {}

fun bar(x: String): Int {}

fun main() {
    foo(::bar)
}
