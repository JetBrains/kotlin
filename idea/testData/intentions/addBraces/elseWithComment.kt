fun foo() {}

fun test(b: Boolean) {
    if (b) foo() <caret>else
        /* aaa */ foo() // bbb
}
