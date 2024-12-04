// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70389
class B<T> {
    fun <R : T> m(x: B<R>) {
        x.m<<!UPPER_BOUND_VIOLATED!>Any<!>>(<!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)
    }
}

class Foo<A> {
    fun <B : A> m(x: Foo<B>?) {
        x?.m<B>(null)
    }
}