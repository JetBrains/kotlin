// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int)
inline class Bar(val y: String)

fun test(f1: Foo, f2: Foo, b1: Bar, fn1: Foo?, fn2: Foo?) {
    val a1 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== f2<!>
    val a2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f1<!>
    val a3 = <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>f1 === b1<!> || <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>f1 !== b1<!>

    val c1 = <!FORBIDDEN_IDENTITY_EQUALS!>fn1 === fn2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn1 !== fn2<!>
    val c2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== fn1<!>
    val c3 = <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>b1 === fn1<!> || <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>b1 !== fn1<!>

    val any = Any()

    val d1 = <!FORBIDDEN_IDENTITY_EQUALS!>any === f1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>any !== f1<!>
    val d2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === any<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== any<!>
    val d3 = <!FORBIDDEN_IDENTITY_EQUALS!>any === fn1<!> || <!FORBIDDEN_IDENTITY_EQUALS!>any !== fn1<!>
    val d4 = <!FORBIDDEN_IDENTITY_EQUALS!>fn1 === any<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn1 !== any<!>
}