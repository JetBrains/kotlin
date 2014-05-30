// IS_APPLICABLE: true
fun foo(x: String) {
    bar<caret><String>(x)
}

fun bar<T>(t: T): Int = 1