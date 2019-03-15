fun <T> getT(): T = null!!

class Test<in I> {
    private fun foo() : I = getT()

    fun apply(<!UNUSED_PARAMETER!>i<!>: I) {}

    init {
        foo()
        this.foo()
    }

    fun test() {
        apply(foo())
        apply(this.foo())
        with(Test<I>()) {
            apply(foo()) // resolved to this@Test.foo
            apply(this.<!INVISIBLE_MEMBER("foo", "private/*private to this*/", "'Test'")!>foo<!>())
            apply(this@with.<!INVISIBLE_MEMBER("foo", "private/*private to this*/", "'Test'")!>foo<!>())
            apply(this@Test.foo())
        }
    }

    fun <I> test(t: Test<I>) {
        t.apply(t.<!INVISIBLE_MEMBER("foo", "private/*private to this*/", "'Test'")!>foo<!>())
    }

    companion object {
        fun <I> test(t: Test<I>) {
            t.apply(t.<!INVISIBLE_MEMBER("foo", "private/*private to this*/", "'Test'")!>foo<!>())
        }
    }
}

fun <I> test(t: Test<I>) {
    t.apply(t.<!INVISIBLE_MEMBER("foo", "private/*private to this*/", "'Test'")!>foo<!>())
}
