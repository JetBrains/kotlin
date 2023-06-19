class Foo {
    private val a: String = "foo"

    inner class Bar {
        private val b: String = "bar"

        fun test() {
            <caret>val x = 0
        }
    }
}

fun callFoo(foo: Foo): Int {
    return 0
}

fun callString(string: String): Int {
    return 1
}