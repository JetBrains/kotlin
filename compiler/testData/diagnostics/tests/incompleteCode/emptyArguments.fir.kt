// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-79116

class A
class B
class C

fun asFunc(x: Int, y: Int, z: Int) { }
fun asVarargFunc(vararg xs: Int) { }
fun asTypedFunc(a: A, b: B, c: C) { }

fun test() {
    val a = A()
    val b = B()
    val c = C()

    asFunc(<!SYNTAX!><!>, 2, 3)
    asFunc(1, 2, <!SYNTAX!><!>,)
    asFunc(1, <!SYNTAX!><!>, 3,)
    asFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asFunc(<!SYNTAX!><!>, <!SYNTAX!><!>,)
    asFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, 3)
    asFunc(<!SYNTAX!><!>, <!NO_VALUE_FOR_PARAMETER!>2)<!>
    asFunc(1, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asFunc(1, <!SYNTAX!><!>,)
    asFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asFunc(1, 2, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asFunc(<!SYNTAX!><!>, 2, <!SYNTAX!><!>, <!SYNTAX!><!>,)

    asVarargFunc(<!SYNTAX!><!>, 2, 3)
    asVarargFunc(<!SYNTAX!><!>, 2, 3,)
    asVarargFunc(1, <!SYNTAX!><!>, 3)
    asVarargFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, 3,)
    asVarargFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asVarargFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asVarargFunc(<!SYNTAX!><!>, 2,)

    asTypedFunc(<!SYNTAX!><!>, b, c,)
    asTypedFunc(<!SYNTAX!><!>, b, c)
    asTypedFunc(a, <!SYNTAX!><!>, c,)
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>,)
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asTypedFunc(<!SYNTAX!><!>, <!NO_VALUE_FOR_PARAMETER!>b,)<!>
    asTypedFunc(a, b, <!SYNTAX!><!>,)
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, c)
    asTypedFunc(a, <!SYNTAX!><!>, <!SYNTAX!><!>,)
    asTypedFunc(a, b, c, <!SYNTAX!><!>,)

    asTypedFunc(a = A(), <!SYNTAX!><!>, c)
    asTypedFunc(c = C(), <!SYNTAX!><!>, <!NO_VALUE_FOR_PARAMETER!>b = B())<!>
    asTypedFunc(<!SYNTAX!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>b<!>,)<!>
    asTypedFunc(<!SYNTAX!><!>, <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!ARGUMENT_PASSED_TWICE!>b<!> = B(), <!NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!SYNTAX!><!>, b = B(), <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    asTypedFunc(a = A(), <!SYNTAX!><!>, c)
    asTypedFunc(a = A(), <!SYNTAX!><!>, c = C(),)
    asTypedFunc(a = A(), <!SYNTAX!><!>, c,)
    asTypedFunc(a = A(), <!SYNTAX!><!>,)
    asTypedFunc(<!SYNTAX!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), b = B(), c = C())
    asTypedFunc(<!SYNTAX!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>b<!>, <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_PASSED_TWICE!>a<!> = A())<!>
    asTypedFunc(<!SYNTAX!><!>, <!SYNTAX!><!>, c = C())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, vararg */
