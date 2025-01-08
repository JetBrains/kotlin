// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74049

// FILE: Inv.java
public class Inv<T> {
    public Inv(T t) {}
}

// FILE: Foo.java
public interface Foo<T> {
    void foo(T t);

    Inv<T> bar();
}

// FILE: main.kt
class FooImpl1<E1> : Foo<E1> {
    override fun foo(t: E1?) {} // OK

    override fun bar(): Inv<E1?> = Inv<E1?>(null)
}

class FooImpl2<E1>(val e: E1) : Foo<E1> {
    override fun foo(t: E1) {} // OK

    override fun bar(): Inv<E1> = Inv(e)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl3<!><E1>(val e: E1) : Foo<E1> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(t: E1 & Any) {} // Should be OK

    override fun bar(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Inv<E1 & Any><!> = Inv(e!!)
}

open class Aside<S>(val s: S) {
    fun foo(t: S & Any) {}

    fun bar(): Inv<S & Any> = Inv(s!!)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class Diamond<!><D>(d: D) : Foo<D>, Aside<D>(d)
