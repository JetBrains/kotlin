fun <T> getT(): T = null!!
fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class Test<in I, out O> {
    private val i: I = getT()

    ;{
        apply(i)
        apply(this.i)
    }

    fun apply(<!UNUSED_PARAMETER!>i<!>: I) {}

    fun test() {
        apply(i)
        apply(this.i)
        with(Test<I, O>()) {
            apply(i) // resolved to this@Test.i
            apply(this.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!>)
            apply(this@with.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!>)
            apply(this@Test.i)
        }
    }

    fun <I, O> test(t: Test<I, O>) {
        t.apply(t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!>)
    }

    default object {
        fun <I, O> test(t: Test<I, O>) {
            t.apply(t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!>)
        }
    }
}

fun <I, O> test(t: Test<I, O>) {
    t.apply(t.<!INVISIBLE_MEMBER(i; private/*private to this*/; Test)!>i<!>)
}