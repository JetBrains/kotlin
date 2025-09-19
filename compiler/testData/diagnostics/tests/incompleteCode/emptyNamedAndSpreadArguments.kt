// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTICS_FULL_TEXT
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
    one(<!SYNTAX!>,<!><!SYNTAX!><!>)
    one(a =<!SYNTAX!><!>)
    one(a =<!SYNTAX!><!>,)

    three(aa =<!SYNTAX!><!>, cc = c, bb = b)
    three(<!SYNTAX!>,<!> bb =<!SYNTAX!><!>, <!NO_VALUE_FOR_PARAMETER!>cc = c)<!>
    three(aa = a, bb = b, cc = c)
    three(aa =<!SYNTAX!><!>, bb =<!SYNTAX!><!>, cc =<!SYNTAX!><!>)
    three(aa =<!SYNTAX!><!>, cc =<!SYNTAX!><!>, bb =<!SYNTAX!><!>)
    three(cc =<!SYNTAX!><!>, aa =<!SYNTAX!><!>, bb = b)
    three(cc =<!SYNTAX!><!>, aa =<!SYNTAX!><!>,)
    three(cc =<!SYNTAX!><!>, aa =<!SYNTAX!><!>, <!SYNTAX!>,<!><!SYNTAX!><!>)

    many(*<!SYNTAX!><!>)
    many(*<!SYNTAX!><!>,)
    many(aa=*<!SYNTAX!><!>)
    many(aa=*<!SYNTAX!><!>,)
    many(<!SYNTAX!>,<!> aa=<!SYNTAX!><!>,)
    many(<!SYNTAX!>,<!> aa=<!SYNTAX!><!>, <!SYNTAX!>,<!><!SYNTAX!><!>)
    many(<!SYNTAX!>,<!> aa=<!SYNTAX!><!>, )
    many(<!SYNTAX!>,<!><!SYNTAX!>,<!><!SYNTAX!><!>)
    many(<!SYNTAX!>,<!><!SYNTAX!><!>)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=*<!SYNTAX!><!>)
    many(<!NAMED_PARAMETER_NOT_FOUND!>bb<!>=<!SYNTAX!><!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, vararg */
