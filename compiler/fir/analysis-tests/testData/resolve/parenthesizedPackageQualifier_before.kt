// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidAnnotationsTypeArgumentsAndParenthesesForPackageQualifier

package p1.p2.p3

fun foo() { }
class Outer {
    object Nested
}

fun test() {
    p1.p2.p3.foo()
    (p1).p2.p3.foo()
    (p1.p2).p3.foo()
    (p1.p2.p3).foo()

    ((p1).p2.p3).Outer.Nested
    ((p1.p2).p3).Outer.Nested
    (((p1).p2).p3).Outer.Nested

    (p1.p2.p3.Outer).Nested
    (p1.p2.p3.Outer)::toString
    (p1.p2.p3.Outer)::class

    (lbl@p1.p2.p3).foo()
    (lbl@p1.p2).p3.foo()
    lbl@(p1.p2).p3.foo()
    lbl@(p1.p2.p3).foo()
    (lbl3@(lbl2@(lbl1@p1.p2).p3)).foo()
    lbl@p1.p2.p3.foo()

    // not reported here, but that's fine: we already have EXPRESSION_EXPECTED_PACKAGE_FOUND
    (p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>)
    (p1.p2.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>p3<!>)::foo

    ((p1.p2.p3).Outer).Nested
    ((p1).p2.p3.Outer).Nested
    (lbl@(p1).p2.p3.Outer).Nested
    (lbl@(p1.p2).p3.Outer.Nested)
    ((p1.p2.p3).foo())
    (lbl@(p1).p2.p3.foo())
}

private val property: Any = ""

fun testWithSmartcast() {
    if (property is String) {
        (p1.p2.p3).property.length
    }

    if (property is Int) {
        (p1.p2).p3.property + 3
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, nestedClass,
objectDeclaration */
