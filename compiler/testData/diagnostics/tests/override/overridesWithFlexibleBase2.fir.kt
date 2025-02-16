// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74049
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

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

class FooImpl<E> : Foo<E> {
    override fun E.foo() {}

    override fun (E & Any).bar() {}
}

class FooImpl2<E> : Foo<E> {
    override fun (E & Any).foo() {}

    override fun (E & Any).bar() {}
}
