// IS_APPLICABLE: true
val x = "x"

fun foo() {
    <caret>bar<String>(x)
}

fun bar<T>(t: T): Int = 1