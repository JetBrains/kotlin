fun <T> getT(): T = null!!
fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class Test<in I, out O> {
    private var i: I = getT()
    private val j: I

    ;{
        j = getT()
        i = getT()
        this.i = getT()
    }

    fun test() {
        i = getT()
        this.i = getT()
        with(Test<I, O>()) {
            i = getT() // resolved to this@Test.i
            this.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!> = getT()
            this@with.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!> = getT()
            this@Test.i  = getT()
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!> = getT()
    }

    default object {
        fun <I, O> test(t: Test<I, O>) {
            t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!> = getT()
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!> = getT()
}