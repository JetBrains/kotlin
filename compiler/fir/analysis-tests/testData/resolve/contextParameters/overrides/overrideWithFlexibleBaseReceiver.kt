// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-75174

// FILE: Base.kt
interface Base<B, B2> {
    context(b: B)
    val prop1: String?

    context(b: B2)
    val B.prop2: String?
}

// FILE: Foo.java
public interface Foo<T, T2> extends Base<T, T2> {
    String getProp1(T t);

    String getProp2(T2 t2, T t);
}

// FILE: main.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl<!><E, E2> : Foo<E, E2> {
    <!NOTHING_TO_OVERRIDE!>override<!> context(b: E) val prop1: String?
        get() = ""

    <!NOTHING_TO_OVERRIDE!>override<!> context(b: E2) val E.prop2: String?
        get() = ""
}
