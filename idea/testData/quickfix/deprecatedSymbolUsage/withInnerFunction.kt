// "Replace with '{ this.bar }()'" "true"

private class A {
    val bar = 1

    @Deprecated("t", ReplaceWith("{ this.bar }()"))
    fun foooo() {
        { bar }()
    }
}

private fun test(a: A) {
    a.<caret>foooo()
}
