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
            apply(<!INVISIBLE_REFERENCE!>foo<!>()) // K1: this@Test.foo, K2: this@with.foo, see KT-55446
            apply(this.<!INVISIBLE_REFERENCE!>foo<!>())
            apply(this@with.<!INVISIBLE_REFERENCE!>foo<!>())
            apply(this@Test.foo())
        }
    }

    fun <I> test(t: Test<I>) {
        t.apply(t.<!INVISIBLE_REFERENCE!>foo<!>())
    }

    companion object {
        fun <I> test(t: Test<I>) {
            t.apply(t.<!INVISIBLE_REFERENCE!>foo<!>())
        }
    }
}

fun <I> test(t: Test<I>) {
    t.apply(t.<!INVISIBLE_REFERENCE!>foo<!>())
}
