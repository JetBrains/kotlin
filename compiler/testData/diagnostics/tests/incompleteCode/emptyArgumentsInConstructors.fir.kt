// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class A
class B
class C

class AsTypedConstructor(a: A, val b: B, c: C = C())

@Repeatable
annotation class AsAnnotationConstructor(val x: Int, val y: String, val z: IntArray)

@AsAnnotationConstructor(<!SYNTAX!><!>, "", [])
@AsAnnotationConstructor(<!SYNTAX!><!>, "", [],)
@AsAnnotationConstructor<!NO_VALUE_FOR_PARAMETER!>(<!SYNTAX!><!>, "",)<!>
@AsAnnotationConstructor(<!SYNTAX!><!>, "", <!SYNTAX!><!>,)
@AsAnnotationConstructor(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
@AsAnnotationConstructor(<!SYNTAX!><!>, <!SYNTAX!><!>,)
@AsAnnotationConstructor(0, <!SYNTAX!><!>,)
@AsAnnotationConstructor(0, "", [],)
@AsAnnotationConstructor(0, <!SYNTAX!><!>, [])
fun test() {
    val a = A()
    val b = B()
    val c = C()

    AsTypedConstructor(<!SYNTAX!><!>, <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!ARGUMENT_TYPE_MISMATCH!>b<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    AsTypedConstructor(a, <!SYNTAX!><!>, c)
    AsTypedConstructor(a, <!SYNTAX!><!>,)
    AsTypedConstructor(a = A(), <!SYNTAX!><!>,)
    AsTypedConstructor(b = B(), a = A(), <!SYNTAX!><!>,)
    AsTypedConstructor(<!SYNTAX!><!>,<!SYNTAX!><!>,<!SYNTAX!><!>,<!SYNTAX!><!>,<!SYNTAX!><!>,)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, functionDeclaration, integerLiteral,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
