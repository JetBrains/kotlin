private class A {
    val bar = 1
    fun <caret>foooo() {
        { bar }()
    }
}

private fun test(a: A) {
    a.foooo()
}