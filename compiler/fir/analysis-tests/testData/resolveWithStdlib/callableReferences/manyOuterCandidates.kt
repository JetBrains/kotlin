fun foo(x: (String) -> Int) {}
fun foo(x: () -> Int) {}


fun bar(): Int = 1

fun main() {
    foo(::bar)
}
