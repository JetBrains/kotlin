// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class A
class B
class C

class AsTypedConstructor(a: A, val b: B, c: C = C())

@Repeatable
annotation class AsAnnotationConstructor(val x: Int, val y: String, val z: IntArray)

@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, "", [])
@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, "", [],)
@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!>"",)<!>
@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, "", <!EMPTY_ARGUMENT!><!>,)
@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
@AsAnnotationConstructor(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
@AsAnnotationConstructor(0, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
@AsAnnotationConstructor(0, "", [],)
@AsAnnotationConstructor(0, <!EMPTY_ARGUMENT!><!>, [])
fun test() {
    val a = A()
    val b = B()
    val c = C()

    AsTypedConstructor(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!ARGUMENT_TYPE_MISMATCH!>b<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    AsTypedConstructor(a, <!EMPTY_ARGUMENT!><!>, c)
    AsTypedConstructor(a, <!EMPTY_ARGUMENT!><!>,)
    AsTypedConstructor(a = A(), <!EMPTY_ARGUMENT!><!>,)
    AsTypedConstructor(b = B(), a = A(), <!EMPTY_ARGUMENT, MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!><!>,)
    AsTypedConstructor(<!EMPTY_ARGUMENT!><!>,<!EMPTY_ARGUMENT!><!>,<!EMPTY_ARGUMENT!><!>,<!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,<!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, functionDeclaration, integerLiteral,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
