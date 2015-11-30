// IS_APPLICABLE: true
fun foo() {
    bar<caret><(Int) -> Int> { it: Int -> it }
}

fun <T> bar(t: T): Int = 1