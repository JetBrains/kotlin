// IS_APPLICABLE: true
fun foo() {
    val x = <caret>bar("x", 0)
}

fun bar<T, V>(t: T, v: V): Int = 1