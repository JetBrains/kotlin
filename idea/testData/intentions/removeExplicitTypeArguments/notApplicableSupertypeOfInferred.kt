// IS_APPLICABLE: false
fun foo() {
    val x = bar<caret><Any>("x")
}

fun bar<T>(t: T): Int = 1