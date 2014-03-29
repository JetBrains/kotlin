// IS_APPLICABLE: false
fun foo() {
    val x = <caret>bar<String>("x")
}

fun bar<T>(t: T): Int = 1