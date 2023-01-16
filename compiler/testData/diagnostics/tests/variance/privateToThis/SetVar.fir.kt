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
            this.<!INVISIBLE_REFERENCE, INVISIBLE_SETTER!>i<!> = getT()
            this@with.<!INVISIBLE_REFERENCE, INVISIBLE_SETTER!>i<!> = getT()
            this@Test.i  = getT()
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.<!INVISIBLE_REFERENCE, INVISIBLE_SETTER!>i<!> = getT()
    }

    companion object {
        fun <I, O> test(t: Test<I, O>) {
            t.<!INVISIBLE_REFERENCE, INVISIBLE_SETTER!>i<!> = getT()
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.<!INVISIBLE_REFERENCE, INVISIBLE_SETTER!>i<!> = getT()
}
