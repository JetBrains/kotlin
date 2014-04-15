// IS_APPLICABLE: false
fun foo() {
    <caret>bar<(Int) -> Int>({ baz(it) })
}

fun baz(x: Int): Int = x

fun bar<T>(t: T): Int = 1