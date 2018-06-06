fun foo(i :Int) {}

fun test(i: Int, b: Boolean) {
    if (i == 1) {<caret>
        if (b) foo(1)
    }
}