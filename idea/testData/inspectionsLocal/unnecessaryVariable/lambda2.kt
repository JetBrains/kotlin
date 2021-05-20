// HIGHLIGHT: INFORMATION
fun test(): Int {
    val <caret>result = foo {
        bar()
    }
    return result
}

fun foo(f: () -> Int) = f()

fun bar() = 1