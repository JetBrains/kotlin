// HIGHLIGHT: GENERIC_ERROR_OR_WARNING
fun test(b: Boolean): Int {
    val <caret>result = if (b)
        foo()
    else
        bar()
    return result
}

fun foo() = 1

fun bar() = 2