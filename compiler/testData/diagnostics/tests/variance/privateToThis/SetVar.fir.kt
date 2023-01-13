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
            i = getT() // resolved to this@Test.i
            <!INVISIBLE_SETTER!>this.<!INVISIBLE_REFERENCE!>i<!><!> = getT()
            <!INVISIBLE_SETTER!>this@with.<!INVISIBLE_REFERENCE!>i<!><!> = getT()
            this@Test.i  = getT()
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        <!INVISIBLE_SETTER!>t.<!INVISIBLE_REFERENCE!>i<!><!> = getT()
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            <!INVISIBLE_SETTER!>t.<!INVISIBLE_REFERENCE!>i<!><!> = getT()
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    <!INVISIBLE_SETTER!>t.<!INVISIBLE_REFERENCE!>i<!><!> = getT()
}
