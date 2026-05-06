// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier

package p1.p2.p3

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Resolved(val int: Int)

fun foo() { }

class Foo {
    object Bar
}

fun test() {
    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Resolved(42) p1)<!>.p2.p3.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Resolved p1.p2)<!>.p3.Foo.Bar
    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Resolved p1.p2.p3)<!>.Foo.Bar
    (@Resolved(42) p1.p2.p3.Foo).Bar
    @Resolved(42) p1.p2.p3.Foo.Bar

    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Unresolved p1)<!>.p2.p3.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Unresolved p1.p2)<!>.p3.Foo.Bar
    <!PARENTHESIZED_PACKAGE_QUALIFIER_WARNING!>(@Unresolved p1.p2.p3)<!>.Foo.Bar
    (@<!UNRESOLVED_REFERENCE!>Unresolved<!> p1.p2.p3.Foo).Bar
    @<!UNRESOLVED_REFERENCE!>Unresolved<!> p1.p2.p3.Foo.Bar

    @<!NO_VALUE_FOR_PARAMETER!>Resolved<!> p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>
    @Resolved(42) p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>
    @<!UNRESOLVED_REFERENCE!>Unresolved<!> p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, nestedClass,
objectDeclaration, primaryConstructor, propertyDeclaration */
