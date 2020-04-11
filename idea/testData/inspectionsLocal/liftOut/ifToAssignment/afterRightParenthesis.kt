fun test(i: Int) {
    val f: () -> Boolean
    <caret>if (i == 1) {
        f = { true }
    } else {
        val foo = foo()
        f = { false }
    }
    f()
}

fun foo() {}