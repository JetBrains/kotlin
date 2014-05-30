// IS_APPLICABLE: true
fun foo() {
    bar<caret><String>("x")
}

fun bar<T>(t: T): Int = 1