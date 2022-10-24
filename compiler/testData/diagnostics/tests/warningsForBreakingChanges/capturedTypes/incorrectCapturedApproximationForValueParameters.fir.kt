// ISSUE: KT-49404
// SKIP_TXT

// FILE: A.java
public class A<T> implements WithExtension<T> {
    public void foo(Inv<T> w) {}
    public void ext(Inv<T> t) {}
}

// FILE: main.kt
class Inv<T>(var t: T)

interface WithExtension<F> {
    fun Inv<F>.ext() {}
}

class B<E> : WithExtension<E> {
    fun foo(w: Inv<E>) {}
}

fun withInvStar(i: Inv<*>.() -> Unit) {}

fun bar1(a: A<in Any>, i: Inv<*>) {
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
}

fun bar2(b: B<in Any>, i: Inv<*>) {
    b.foo(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
}

fun A<in Any>.bar3(i: Inv<*>) {
    i.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>ext<!>()
    withInvStar {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>ext<!>()
    }
}

fun B<in Any>.bar4(i: Inv<*>) {
    i.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>ext<!>()
    withInvStar {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>ext<!>()
    }
}
