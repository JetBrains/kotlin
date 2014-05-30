// IS_APPLICABLE: true
val x = "x"

fun foo() {
    bar<caret><String>(x)
}

fun bar<T>(t: T): Int = 1