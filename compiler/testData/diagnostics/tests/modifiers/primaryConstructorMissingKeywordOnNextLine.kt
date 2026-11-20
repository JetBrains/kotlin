// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90782
// DIAGNOSTICS: -UNUSED_PARAMETER
annotation class Ann

class A1 <!MISSING_CONSTRUCTOR_KEYWORD!>private (val x: Int)<!>

class A2
<!MISSING_CONSTRUCTOR_KEYWORD!>private (val x: Int)<!>

class A3
<!MISSING_CONSTRUCTOR_KEYWORD!>@Ann (val x: Int)<!>

fun foo() {
    class C1 <!MISSING_CONSTRUCTOR_KEYWORD!>private (x: Int)<!>

    class C2
    <!UNRESOLVED_REFERENCE!>private<!> (<!UNRESOLVED_REFERENCE!>x<!><!SYNTAX!>: Int<!>)

    class C3
    <!WRONG_ANNOTATION_TARGET!>@Ann<!> <!UNRESOLVED_REFERENCE!>private<!> (<!UNRESOLVED_REFERENCE!>x<!><!SYNTAX!>: Int<!>)
}

class Outer {
    class Nested
    <!MISSING_CONSTRUCTOR_KEYWORD!>private (val x: Int)<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, localClass, primaryConstructor,
propertyDeclaration */
