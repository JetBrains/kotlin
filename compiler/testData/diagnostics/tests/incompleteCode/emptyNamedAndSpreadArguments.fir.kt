// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-79116

class A
class B
class C

fun one(a: A) {}
fun three(aa: A, bb: B, cc: C) {}
fun many(vararg aa: A) {}

fun test() {
    val a = A()
    val b = B()
    val c = C()

    one<!NO_VALUE_FOR_PARAMETER!>()<!>
    one(<!EMPTY_ARGUMENT!><!>,)
    one(a =<!EMPTY_ARGUMENT!><!>)
    one(a =<!EMPTY_ARGUMENT!><!>,)

    three(aa =<!EMPTY_ARGUMENT!><!>, cc = c, bb = b)
    three(<!EMPTY_ARGUMENT!><!>, bb =<!EMPTY_ARGUMENT!><!>, cc = c)
    three(aa = a, bb = b, cc = c)
    three(aa =<!EMPTY_ARGUMENT!><!>, bb =<!EMPTY_ARGUMENT!><!>, cc =<!EMPTY_ARGUMENT!><!>)
    three(aa =<!EMPTY_ARGUMENT!><!>, cc =<!EMPTY_ARGUMENT!><!>, bb =<!EMPTY_ARGUMENT!><!>)
    three(cc =<!EMPTY_ARGUMENT!><!>, aa =<!EMPTY_ARGUMENT!><!>, bb = b)
    three(cc =<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!>aa =<!EMPTY_ARGUMENT!><!>,)<!>
    three(cc =<!EMPTY_ARGUMENT!><!>, aa =<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT, MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!><!>,)<!>

    many(<!SPREAD_OF_NULLABLE!>*<!><!EMPTY_ARGUMENT!><!>)
    many(<!SPREAD_OF_NULLABLE!>*<!><!EMPTY_ARGUMENT!><!>,)
    many(aa=<!SPREAD_OF_NULLABLE!>*<!><!EMPTY_ARGUMENT, REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!><!>)
    many(aa=<!SPREAD_OF_NULLABLE!>*<!><!EMPTY_ARGUMENT, REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!><!>,)
    many(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!EMPTY_ARGUMENT!><!>,)
    many(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT, MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!><!>,)
    many(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!EMPTY_ARGUMENT!><!>, )
    many(<!EMPTY_ARGUMENT!><!>,<!EMPTY_ARGUMENT!><!>,)
    many(<!EMPTY_ARGUMENT!><!>,)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=<!SPREAD_OF_NULLABLE!>*<!><!EMPTY_ARGUMENT!><!>)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=<!EMPTY_ARGUMENT!><!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, vararg */
