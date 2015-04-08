// IS_APPLICABLE: true
fun foo() {
    bar<caret><(Int) -> Int> { it: Int -> it }
}

fun bar<T>(t: T): Int = 1