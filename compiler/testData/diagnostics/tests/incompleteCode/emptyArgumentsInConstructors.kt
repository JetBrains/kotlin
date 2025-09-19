// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class A
class B
class C

class AsTypedConstructor(a: A, val b: B, c: C = C())

@Repeatable
annotation class AsAnnotationConstructor(val x: Int, val y: String, val z: IntArray)

@AsAnnotationConstructor(<!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>""<!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>[]<!>)<!>
@AsAnnotationConstructor(<!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>""<!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>[]<!>,)<!>
@AsAnnotationConstructor(<!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>""<!>,)<!>
@AsAnnotationConstructor(<!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>""<!>, <!SYNTAX!>,<!><!SYNTAX!><!>)
@AsAnnotationConstructor(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
@AsAnnotationConstructor(<!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
@AsAnnotationConstructor(0, <!SYNTAX!>,<!><!SYNTAX!><!>)
@AsAnnotationConstructor(0, "", [],)
@AsAnnotationConstructor(0, <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>[]<!>)<!>
fun test() {
    val a = A()
    val b = B()
    val c = C()

    AsTypedConstructor(<!SYNTAX!>,<!> a, b, c)
    AsTypedConstructor(a, <!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>c<!>)
    AsTypedConstructor(a, <!SYNTAX!>,<!><!SYNTAX!><!>)
    AsTypedConstructor(a = A(), <!SYNTAX!>,<!><!SYNTAX!><!>)
    AsTypedConstructor(b = B(), a = A(), <!SYNTAX!>,<!><!SYNTAX!><!>)
    AsTypedConstructor(<!SYNTAX!>,<!><!SYNTAX!>,<!><!SYNTAX!>,<!><!SYNTAX!>,<!><!SYNTAX!>,<!><!SYNTAX!><!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, functionDeclaration, integerLiteral,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
