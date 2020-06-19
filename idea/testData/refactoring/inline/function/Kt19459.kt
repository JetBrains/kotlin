private class A {
    val bar = 1
    val parent: A
        get() = null!!
}

fun <T> myrun(f: () -> T) = f()

private fun A.<caret>foo() = myrun { bar }

private fun test(a: A) {
    a.foo()
}