// FIR_COMPARISON
class A<in I> {
    private val bar: I

    private fun foo(): I = null!!


    fun test() {
        <caret>
    }
}

// INVOCATION_COUNT: 1
// EXIST: bar, foo
