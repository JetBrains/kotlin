// FILE: JA.java
public interface JA<E> {
    public E getFoo();
}

// FILE: main.kt

interface KB<F> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun getFoo(): F
    <!NOTHING_TO_OVERRIDE!>override<!> fun getBar(): F
}

interface D1 : JA<String>, KB<String>
interface E1 : D1 {
    override fun getFoo(): String
    override fun getBar(): String
}

interface D2 : KB<String>, JA<String>
interface E2 : D2 {
    override fun getFoo(): String
    override fun getBar(): String
}

fun main(
    d1: D1, e1: E1,
    d2: D2, e2: E2,
) {
    d1.foo
    d1.<!UNRESOLVED_REFERENCE!>bar<!>
    e1.foo
    e1.<!UNRESOLVED_REFERENCE!>bar<!>

    d2.foo
    d2.<!UNRESOLVED_REFERENCE!>bar<!>
    e2.foo
    e2.<!UNRESOLVED_REFERENCE!>bar<!>
}
