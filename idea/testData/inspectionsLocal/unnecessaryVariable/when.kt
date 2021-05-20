// HIGHLIGHT: INFORMATION
fun test(i: Int): Int {
    val <caret>result = when (i) {
        1 -> foo()
        else -> bar()
    }
    return result
}

fun foo() = 1

fun bar() = 2