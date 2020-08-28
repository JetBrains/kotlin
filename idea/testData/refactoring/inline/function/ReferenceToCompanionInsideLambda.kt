private class A {
    fun <caret>foooo() {
        { bar }()
    }

    companion object {
        const val bar = 4
    }
}

private fun test(a: A) {
    a.foooo()
}