// IS_APPLICABLE: true
fun foo(x: String) {
    <caret>bar<String>(x)
}

fun bar<T>(t: T): Int = 1