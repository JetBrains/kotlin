// IS_APPLICABLE: true
fun foo() {
    <caret>bar<String>("x")
}

fun bar<T>(t: T): Int = 1