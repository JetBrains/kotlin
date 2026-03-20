// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// LANGUAGE: +ValueClasses
// DIAGNOSTICS: -UNUSED_VARIABLE

value class Foo(val x: Int)

abstract value class Bar1(y: String)
sealed value class Bar2(y: String)
<!VALUE_CLASS_OPEN!>open<!> value class Bar3(y: String)

fun test(f1: Foo, f2: Foo, b1: Bar1, b2: Bar2, b3: Bar3, fn1: Foo?, fn2: Foo?) {
    val a1 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== f2<!>
    val a2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f1<!>
    val a3 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === b1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== b1<!>
    val a4 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === b2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== b2<!>
    val a5 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === b3<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== b3<!>

    val c1 = <!FORBIDDEN_IDENTITY_EQUALS!>fn1 === fn2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn1 !== fn2<!>
    val c2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== fn1<!>
    val c3 = <!FORBIDDEN_IDENTITY_EQUALS!>b1 === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>b1 !== fn1<!>
    val c4 = <!FORBIDDEN_IDENTITY_EQUALS!>b2 === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>b2 !== fn1<!>
    val c5 = <!FORBIDDEN_IDENTITY_EQUALS!>b3 === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>b3 !== fn1<!>

    val any = Any()

    val d1 = <!FORBIDDEN_IDENTITY_EQUALS!>any === f1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>any !== f1<!>
    val d2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === any<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== any<!>
    val d3 = <!FORBIDDEN_IDENTITY_EQUALS!>any === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>any !== fn1<!>
    val d4 = <!FORBIDDEN_IDENTITY_EQUALS!>fn1 === any<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn1 !== any<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, disjunctionExpression, equalityExpression,
functionDeclaration, localProperty, nullableType, primaryConstructor, propertyDeclaration, value */
