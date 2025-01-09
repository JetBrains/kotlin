// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74049
// LANGUAGE: -AllowDnnTypeOverridingFlexibleType

// FILE: Base.kt
interface Base<B> {
    fun B.foo()

    fun (B & Any).bar()
}

// FILE: Foo.java
public interface Foo<T> extends Base<T> {
    void foo(T t);

    void bar(T t);
}

// FILE: main.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl<!><E> : Foo<E> {
    override fun E.foo() {}

    <!NOTHING_TO_OVERRIDE!>override<!> fun (E & Any).bar() {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl2<!><E> : Foo<E> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun (E & Any).foo() {}

    <!NOTHING_TO_OVERRIDE!>override<!> fun (E & Any).bar() {}
}
