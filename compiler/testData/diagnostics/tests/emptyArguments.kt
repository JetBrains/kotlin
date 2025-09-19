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

    asFunc(<!ARGUMENT_EXPECTED!><!>, 2, 3)
    asFunc(1, 2, <!ARGUMENT_EXPECTED!><!>,)
    asFunc(1, <!ARGUMENT_EXPECTED!><!>, 3,)
    asFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asFunc(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
    asFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, 3)
    asFunc(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!>2)<!>
    asFunc(1, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asFunc(1, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
    asFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)
    asFunc(1, 2, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)
    asFunc(<!ARGUMENT_EXPECTED!><!>, 2, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)

    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, 2, 3)
    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, 2, 3,)
    asVarargFunc(1, <!ARGUMENT_EXPECTED!><!>, 3)
    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, 3,)
    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asVarargFunc(<!ARGUMENT_EXPECTED!><!>, 2,)

    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, b, c,)
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, b, c)
    asTypedFunc(a, <!ARGUMENT_EXPECTED!><!>, c,)
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!>b,)<!>
    asTypedFunc(a, b, <!ARGUMENT_EXPECTED!><!>,)
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, c)
    asTypedFunc(a, <!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>,)
    asTypedFunc(a, b, c, <!ARGUMENT_EXPECTED, TOO_MANY_ARGUMENTS!><!>,)

    asTypedFunc(a = A(), <!ARGUMENT_EXPECTED!><!>, c)
    asTypedFunc(c = C(), <!ARGUMENT_EXPECTED, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!><!>, <!NO_VALUE_FOR_PARAMETER!>b = B())<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>b<!>,)<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!TYPE_MISMATCH!>a<!>, <!ARGUMENT_PASSED_TWICE!>b<!> = B(), <!NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, b = B(), <!TYPE_MISMATCH!>a<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    asTypedFunc(a = A(), <!ARGUMENT_EXPECTED!><!>, c)
    asTypedFunc(a = A(), <!ARGUMENT_EXPECTED!><!>, c = C(),)
    asTypedFunc(a = A(), <!ARGUMENT_EXPECTED!><!>, c,)
    asTypedFunc(a = A(), <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_EXPECTED!><!>,)<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), b = B(), c = C())
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>b<!>, <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_PASSED_TWICE!>a<!> = A())<!>
    asTypedFunc(<!ARGUMENT_EXPECTED!><!>, <!ARGUMENT_EXPECTED!><!>, c = C())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, vararg */
