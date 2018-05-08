// IS_APPLICABLE: false

fun foo(a: Int = 1, b: Int = 1, c: Int = 1) {}

fun bar(a: Int, b: Int, c: Int) {
    foo(a, b, c<caret>)
}