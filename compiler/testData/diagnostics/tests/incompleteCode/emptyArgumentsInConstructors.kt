// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class A
class B
class C

class AsTypedConstructor(a: A, val b: B, c: C = C())

@Repeatable
annotation class AsAnnotationConstructor(val x: Int, val y: String, val z: IntArray)

@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, "", [])
@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, "", [],)
@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!>"",)<!>
@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, "", <!ARGUMENT_EXPECTED!><!>,)
@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
@AsAnnotationConstructor(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
@AsAnnotationConstructor(0, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
@AsAnnotationConstructor(0, "", [],)
@AsAnnotationConstructor(0, <!ARGUMENT_EXPECTED!><!>, [])
fun test() {
    val a = A()
    val b = B()
    val c = C()

    AsTypedConstructor(<!ARGUMENT_EXPECTED!><!>, <!TYPE_MISMATCH!>a<!>, <!TYPE_MISMATCH!>b<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    AsTypedConstructor(a, <!ARGUMENT_EXPECTED!><!>, c)
    AsTypedConstructor(a, <!ARGUMENT_EXPECTED!><!>,)
    AsTypedConstructor(a = A(), <!ARGUMENT_EXPECTED!><!>,)
    AsTypedConstructor(b = B(), a = A(), <!ARGUMENT_EXPECTED, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!><!>,)
    AsTypedConstructor(<!ARGUMENT_EXPECTED!><!>,<!ARGUMENT_EXPECTED!><!>,<!ARGUMENT_EXPECTED!><!>,<!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,<!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, functionDeclaration, integerLiteral,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
