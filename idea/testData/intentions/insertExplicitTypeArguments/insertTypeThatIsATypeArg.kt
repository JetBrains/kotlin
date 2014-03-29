// IS_APPLICABLE: true
fun foo<T>(t: T) {
    <caret>bar(t)
}

fun bar<T>(t: T): Int = 1