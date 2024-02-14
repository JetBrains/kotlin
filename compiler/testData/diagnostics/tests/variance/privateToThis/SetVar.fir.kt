fun <T> getT(): T = null!!

class Test<in I, out O> {
    private var i: I = getT()
    private val j: I

    init {
        j = getT()
        i = getT()
        this.i = getT()
    }

    fun test() {
        i = getT()
        this.i = getT()
        with(Test<I, O>()) {
            <!INVISIBLE_REFERENCE!>i<!> = getT() // K1: this@Test.i, K2: this@with.i, see KT-55446
            this.<!INVISIBLE_REFERENCE!>i<!> = getT()
            this@with.<!INVISIBLE_REFERENCE!>i<!> = getT()
            this@Test.i  = getT()
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.<!INVISIBLE_REFERENCE!>i<!> = getT()
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            t.<!INVISIBLE_REFERENCE!>i<!> = getT()
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.<!INVISIBLE_REFERENCE!>i<!> = getT()
}
