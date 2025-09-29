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

    asFunc(<!EMPTY_ARGUMENT!><!>, 2, 3)
    asFunc(1, 2, <!EMPTY_ARGUMENT!><!>,)
    asFunc(1, <!EMPTY_ARGUMENT!><!>, 3,)
    asFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asFunc(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
    asFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, 3)
    asFunc(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!>2)<!>
    asFunc(1, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asFunc(1, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
    asFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)
    asFunc(1, 2, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)
    asFunc(<!EMPTY_ARGUMENT!><!>, 2, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)

    asVarargFunc(<!EMPTY_ARGUMENT!><!>, 2, 3)
    asVarargFunc(<!EMPTY_ARGUMENT!><!>, 2, 3,)
    asVarargFunc(1, <!EMPTY_ARGUMENT!><!>, 3)
    asVarargFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, 3,)
    asVarargFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asVarargFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asVarargFunc(<!EMPTY_ARGUMENT!><!>, 2,)

    asTypedFunc(<!EMPTY_ARGUMENT!><!>, b, c,)
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, b, c)
    asTypedFunc(a, <!EMPTY_ARGUMENT!><!>, c,)
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!>b,)<!>
    asTypedFunc(a, b, <!EMPTY_ARGUMENT!><!>,)
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, c)
    asTypedFunc(a, <!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>,)
    asTypedFunc(a, b, c, <!EMPTY_ARGUMENT, TOO_MANY_ARGUMENTS!><!>,)

    asTypedFunc(a = A(), <!EMPTY_ARGUMENT!><!>, c)
    asTypedFunc(c = C(), <!EMPTY_ARGUMENT, MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!><!>, <!NO_VALUE_FOR_PARAMETER!>b = B())<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>b<!>,)<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!ARGUMENT_PASSED_TWICE!>b<!> = B(), <!NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, b = B(), <!ARGUMENT_TYPE_MISMATCH!>a<!>, <!TOO_MANY_ARGUMENTS!>c<!>)
    asTypedFunc(a = A(), <!EMPTY_ARGUMENT!><!>, c)
    asTypedFunc(a = A(), <!EMPTY_ARGUMENT!><!>, c = C(),)
    asTypedFunc(a = A(), <!EMPTY_ARGUMENT!><!>, c,)
    asTypedFunc(a = A(), <!NO_VALUE_FOR_PARAMETER!><!EMPTY_ARGUMENT!><!>,)<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), b = B(), c = C())
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!ARGUMENT_PASSED_TWICE!>a<!> = A(), <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>b<!>, <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>c<!>)<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_PASSED_TWICE!>a<!> = A())<!>
    asTypedFunc(<!EMPTY_ARGUMENT!><!>, <!EMPTY_ARGUMENT!><!>, c = C())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, vararg */
