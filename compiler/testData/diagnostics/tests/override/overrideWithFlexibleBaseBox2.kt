// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74823
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: KotlinBox.kt
class KotlinBox<T>

// FILE: KotlinBase.kt
interface KotlinBase<T> {
    fun test1(a: KotlinBox<out T>)
    fun test2(a: KotlinBox<in T>)
}

// FILE: Foo.java
public interface Foo<T> extends KotlinBase<T> {}

// FILE: main.kt
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl1<!><E1> : Foo<E1> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun test1(a: KotlinBox<out E1 & Any>) {}

    <!NOTHING_TO_OVERRIDE!>override<!> fun test2(a: KotlinBox<in E1 & Any>) {}
}
