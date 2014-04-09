// IS_APPLICABLE: true
fun foo() {
    <caret>bar({ 2 * it }: (Int) -> Int)
}

fun bar<T>(t: T): Int = 1