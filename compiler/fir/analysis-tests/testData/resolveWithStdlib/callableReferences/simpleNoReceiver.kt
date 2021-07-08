fun foo(x: (String) -> Int) {}

fun bar(x: String): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun main() {
    foo(::bar)
}
