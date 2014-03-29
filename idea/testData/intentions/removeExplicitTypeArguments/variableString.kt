// IS_APPLICABLE: true
fun foo() {
    val x = "x"
    <caret>bar<String>(x)
}

fun bar<T>(t: T): Int = 1