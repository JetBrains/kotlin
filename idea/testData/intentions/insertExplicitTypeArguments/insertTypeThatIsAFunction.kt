// IS_APPLICABLE: true
fun foo() {
    <caret>bar({ 2 * it } as (Int) -> Int)
}

fun bar<T>(t: T): Int = 1