// RUN_PIPELINE_TILL: FRONTEND
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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl<!><E> : Foo<E> {
    override val E.prop1: String?
        get() = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl2<!><E> : Foo<E> {
    override val E?.prop1: String?
        get() = ""
}
