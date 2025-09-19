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

    asFunc(<!SYNTAX!>,<!> 2, <!NO_VALUE_FOR_PARAMETER!>3)<!>
    asFunc(1, 2, <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(1, <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!>3,)<!>
    asFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>3)<!>
    asFunc(<!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>2)<!>
    asFunc(1, <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(1, <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(1, 2, <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asFunc(<!SYNTAX!>,<!> 2, <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)

    asVarargFunc(<!SYNTAX!>,<!> 2, 3)
    asVarargFunc(<!SYNTAX!>,<!> 2, 3,)
    asVarargFunc(1, <!SYNTAX!>,<!> 3)
    asVarargFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> 3,)
    asVarargFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asVarargFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asVarargFunc(<!SYNTAX!>,<!> 2,)

    asTypedFunc(<!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>b<!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>,)<!>
    asTypedFunc(<!SYNTAX!>,<!> <!ARGUMENT_TYPE_MISMATCH!>b<!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>)<!>
    asTypedFunc(a, <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>,)<!>
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(<!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>b<!>,)<!>
    asTypedFunc(a, b, <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>)<!>
    asTypedFunc(a, <!SYNTAX!>,<!> <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(a, b, c, <!SYNTAX!>,<!><!SYNTAX!><!>)

    asTypedFunc(a = A(), <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>)<!>
    asTypedFunc(c = C(), <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!>b = B())<!>
    asTypedFunc(<!SYNTAX!>,<!> a = A(), <!NO_VALUE_FOR_PARAMETER!>b,)<!>
    asTypedFunc(<!SYNTAX!>,<!> a, b = B(), c)
    asTypedFunc(<!SYNTAX!>,<!> b = B(), <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>a<!>, <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>c<!>)<!>
    asTypedFunc(a = A(), <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>)<!>
    asTypedFunc(a = A(), <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!>c = C(),)<!>
    asTypedFunc(a = A(), <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>c<!>,)<!>
    asTypedFunc(a = A(), <!SYNTAX!>,<!><!SYNTAX!><!>)
    asTypedFunc(<!SYNTAX!>,<!> a = A(), b = B(), c = C())
    asTypedFunc(<!SYNTAX!>,<!> a = A(), b, c)
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>a = A())<!>
    asTypedFunc(<!SYNTAX!>,<!> <!SYNTAX!>,<!> <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>c = C())<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, vararg */
