// IS_APPLICABLE: false

fun foo(i :Int) {}

fun test(i: Int, b: Boolean) {
    if (i == 1) {<caret>
        if (b) foo(1)
    } else {
        if (b) foo(2)
    }
}