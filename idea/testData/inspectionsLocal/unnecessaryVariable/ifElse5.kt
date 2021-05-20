// HIGHLIGHT: INFORMATION
fun test(b: Boolean): Int {
    val <caret>result = if (b)
        baz {
            foo()
        }
    else
        baz {
            bar()
        }
    return result
}

fun foo() = 1

fun bar() = 2

fun baz(f: () -> Int) = f()