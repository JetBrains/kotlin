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
class FooImpl<E> : Foo<E> {

    override val (E & Any).prop1: String
        get() = ""

    override val (E & Any).prop2: String
        get() = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl2<!><E> : Foo<E> {

    override val E.prop1: String
        get() = ""

    <!NOTHING_TO_OVERRIDE!>override<!> val E.prop2: String
        get() = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class FooImpl3<!><E> : Foo<E> {
    override val E?.prop1: String
        get() = ""

    <!NOTHING_TO_OVERRIDE!>override<!> val E?.prop2: String
        get() = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, getter, interfaceDeclaration, javaType, nullableType, override,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral, typeParameter */
