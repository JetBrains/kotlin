// IS_APPLICABLE: true
fun foo() {
    val x = "x"
    bar<caret><String>(x)
}

fun bar<T>(t: T): Int = 1