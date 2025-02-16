// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74049
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: Inv.java
public class Inv<T> {
    public Inv(T t) {}
}

// FILE: Base.kt

interface Base<B> {
    val x: Inv<B>

    val y: Inv<B & Any>
}

// FILE: Foo.java
public interface Foo<T> extends Base<T> {
    Inv<T> getX();

    Inv<T> getY();
}

// FILE: main.kt
class FooImpl<E>(val e: E) : Foo<E> {
    override val x: Inv<E> get() = Inv(e)

    override val y: Inv<E & Any> get() = Inv(e!!)
}

class FooImpl2<E>(val e: E) : Foo<E> {
    override val x: Inv<E & Any> get() = Inv(e!!)

    override val y: Inv<E & Any> get() = Inv(e!!)
}
