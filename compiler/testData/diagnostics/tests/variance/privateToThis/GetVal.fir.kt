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
            apply(this.<!INVISIBLE_REFERENCE!>i<!>)
            apply(this@with.<!INVISIBLE_REFERENCE!>i<!>)
            apply(this@Test.i)
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.apply(t.<!INVISIBLE_REFERENCE!>i<!>)
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            t.apply(t.<!INVISIBLE_REFERENCE!>i<!>)
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.apply(t.<!INVISIBLE_REFERENCE!>i<!>)
}
