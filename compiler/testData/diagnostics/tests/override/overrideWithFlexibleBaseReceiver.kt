// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74920
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: Base.kt
interface Base<B> {
    val B.prop1 : String

    val (B & Any).prop2 : String
}

// FILE: Foo.java
public interface Foo<T> extends Base<T> {
    String getProp1(T t);

    String getProp2(T t);
}

// FILE: main.kt
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl<!><E> : Foo<E> {

    <!NOTHING_TO_OVERRIDE!>override<!> val (E & Any).prop1: String
        get() = ""

    <!NOTHING_TO_OVERRIDE!>override<!> val (E & Any).prop2: String
        get() = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'FooImpl2'; public abstract fun getProp1(t: E!): String! defined in Foo")!>class FooImpl2<!><E> : Foo<E> {

    override val E.prop1: String
        get() = ""

    override val E.prop2: String
        get() = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED("Class 'FooImpl3'; public abstract val E.prop2: String defined in Foo")!>class FooImpl3<!><E> : Foo<E> {
    override val E?.prop1: String
        get() = ""

    <!NOTHING_TO_OVERRIDE!>override<!> val E?.prop2: String
        get() = ""
}
