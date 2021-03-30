fun test(i: Int) {
    val f: () -> Boolean
    <caret>if (i == 1) {
        f = { true }
    } else {
        val foo = Foo().foo { i } // comment
        f = { false }
    }
    f()
}

class Foo {
    fun foo(f: () -> Int) {}
}
