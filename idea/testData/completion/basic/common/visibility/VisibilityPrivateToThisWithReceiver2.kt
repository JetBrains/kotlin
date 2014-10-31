class A<in I> {
    private val bar: I

    private fun foo(): I = null!!


    fun test(a: A<Int>) {
        a.<caret>
    }
}

// INVOCATION_COUNT: 2
// EXIST: bar, foo
