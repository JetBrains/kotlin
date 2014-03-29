// IS_APPLICABLE: true
fun foo() {
    <caret>bar(1, 2, 3, 4)
}

fun bar<T>(vararg ts: T): Int = 1