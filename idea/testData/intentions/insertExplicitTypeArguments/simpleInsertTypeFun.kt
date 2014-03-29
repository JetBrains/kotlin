// IS_APPLICABLE: true
fun foo() {
    <caret>bar("x")
}

fun bar<T>(t: T): Int = 1