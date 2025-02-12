// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74823
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: KotlinBox.kt
class KotlinBox<T>

// FILE: Foo.java
public interface Foo<T> {
    void foo(KotlinBox<? extends T> t);
}

// FILE: main.kt
class FooImpl1<E1> : Foo<E1> {
    override fun foo(t: KotlinBox<out E1?>) {}
}

class FooImpl2<E1> : Foo<E1> {
    override fun foo(t: KotlinBox<out E1>) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl3<!><E1> : Foo<E1> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(t: KotlinBox<out E1 & Any>) {}
}
