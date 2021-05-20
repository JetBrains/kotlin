// HIGHLIGHT: INFORMATION
fun test(): Int {
    val <caret>result = try {
        foo()
    } finally {
        bar()
    }
    return result
}

fun foo() = 1

fun bar() {}