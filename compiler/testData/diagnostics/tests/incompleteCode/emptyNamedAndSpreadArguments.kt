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
    one(<!ARGUMENT_EXPECTED!><!>,)
    one(a =<!ARGUMENT_EXPECTED!><!>)
    one(a =<!ARGUMENT_EXPECTED!><!>,)

    three(aa =<!ARGUMENT_EXPECTED!><!>, cc = c, bb = b)
    three(<!ARGUMENT_EXPECTED!><!>, bb =<!ARGUMENT_EXPECTED!><!>, cc = c)
    three(aa = a, bb = b, cc = c)
    three(aa =<!ARGUMENT_EXPECTED!><!>, bb =<!ARGUMENT_EXPECTED!><!>, cc =<!ARGUMENT_EXPECTED!><!>)
    three(aa =<!ARGUMENT_EXPECTED!><!>, cc =<!ARGUMENT_EXPECTED!><!>, bb =<!ARGUMENT_EXPECTED!><!>)
    three(cc =<!ARGUMENT_EXPECTED!><!>, aa =<!ARGUMENT_EXPECTED!><!>, bb = b)
    three(cc =<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!>aa =<!ARGUMENT_EXPECTED!><!>,)<!>
    three(cc =<!ARGUMENT_EXPECTED!><!>, aa =<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!><!>,)<!>

    many(*<!ARGUMENT_EXPECTED!><!>)
    many(*<!ARGUMENT_EXPECTED!><!>,)
    many(aa=*<!ARGUMENT_EXPECTED!><!>)
    many(aa=*<!ARGUMENT_EXPECTED!><!>,)
    many(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!ARGUMENT_EXPECTED!><!>,)
    many(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!><!>,)
    many(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>aa<!>=<!ARGUMENT_EXPECTED!><!>, )
    many(<!ARGUMENT_EXPECTED!><!>,<!ARGUMENT_EXPECTED!><!>,)
    many(<!ARGUMENT_EXPECTED!><!>,)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=*<!ARGUMENT_EXPECTED!><!>)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=<!ARGUMENT_EXPECTED!><!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, vararg */
