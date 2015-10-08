// IS_APPLICABLE: true
fun foo() {
    <caret>bar({ i: Int -> 2 * i })
}

fun bar<T>(t: T): Int = 1