fun <T> getT(): T = null!!

class Test<in I> {
    private fun foo() : I = getT()

    fun apply(i: I) {}

    init {
        foo()
        this.foo()
    }

    fun test() {
        apply(foo())
        apply(this.foo())
        with(Test<I>()) {
            apply(foo()) // resolved to this@Test.foo
            apply(this.foo())
            apply(this@with.foo())
            apply(this@Test.foo())
        }
    }

    fun <I> test(t: Test<I>) {
        t.apply(t.foo())
    }

    companion object {
        fun <I> test(t: Test<I>) {
            t.apply(t.foo())
        }
    }
}

fun <I> test(t: Test<I>) {
    t.apply(t.<!HIDDEN!>foo<!>())
}
