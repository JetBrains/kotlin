// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75174

// FILE: Base.kt
interface Base<B> {
    val B.prop1 : String?
}

// FILE: Foo.java
public interface Foo<T> extends Base<T> {
    String getProp1(T t);
}

// FILE: main.kt

class FooImpl<E> : Foo<E> {
    override val E.prop1: String?
        get() = ""
}

class FooImpl2<E> : Foo<E> {
    override val E?.prop1: String?
        get() = ""
}
