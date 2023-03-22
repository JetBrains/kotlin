// IGNORE_REVERSED_RESOLVE
fun <T> getT(): T = null!!

class Test<in I, out O> {
    private val i: I = getT()

    init {
        apply(i)
        apply(this.i)
    }

    fun apply(i: I) {}

    fun test() {
        apply(i)
        apply(this.i)
        with(Test<I, O>()) {
            apply(i) // resolved to this@Test.i
            apply(this.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
            apply(this@with.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
            apply(this@Test.i)
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.apply(t.<!INVISIBLE_MEMBER("i; private/*private to this*/; 'Test'")!>i<!>)
}
