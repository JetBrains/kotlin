// IS_APPLICABLE: false
fun foo() {
    val x = bar("<caret>x")
}

fun bar<T>(t: T): Int = 1