// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier

package p1.p2.p3

fun foo() { }
class Outer {
    object Nested
}

fun test() {
    p1.p2.p3.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1)<!>.p2.p3.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2)<!>.p3.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2.p3)<!>.foo()

    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>((p1).p2.p3)<!>.Outer.Nested
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>((p1.p2).p3)<!>.Outer.Nested
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(((p1).p2).p3)<!>.Outer.Nested

    (p1.p2.p3.Outer).Nested
    (p1.p2.p3.Outer)::toString
    (p1.p2.p3.Outer)::class

    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(lbl@p1.p2.p3)<!>.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(lbl@p1.p2)<!>.p3.foo()
    lbl@<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2)<!>.p3.foo()
    lbl@<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2.p3)<!>.foo()
    <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(lbl3@(lbl2@(lbl1@p1.p2).p3))<!>.foo()
    lbl@p1.p2.p3.foo()

    // not reported here, but that's fine: we already have EXPRESSION_EXPECTED_PACKAGE_FOUND
    (p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>)
    (p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>)::foo

    (<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2.p3)<!>.Outer).Nested
    (<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1)<!>.p2.p3.Outer).Nested
    (lbl@<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1)<!>.p2.p3.Outer).Nested
    (lbl@<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2)<!>.p3.Outer.Nested)
    (<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2.p3)<!>.foo())
    (lbl@<!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1)<!>.p2.p3.foo())
}

private val property: Any = ""

fun testWithSmartcast() {
    if (property is String) {
        <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2.p3)<!>.property.length
    }

    if (property is Int) {
        <!PARENTHESIZED_PACKAGE_QUALIFIER_ERROR!>(p1.p2)<!>.p3.property + 3
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, nestedClass,
objectDeclaration */
