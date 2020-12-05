// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)
@JvmInline
value class Bar(val y: String)

fun test(f1: Foo, f2: Foo, b1: Bar, fn1: Foo?, fn2: Foo?) {
    val a1 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>f1 !== f2<!>
    val a2 = <!FORBIDDEN_IDENTITY_EQUALS!>f1 === f1<!>
    val a3 = <!EQUALITY_NOT_APPLICABLE!>f1 === b1<!> || <!EQUALITY_NOT_APPLICABLE!>f1 !== b1<!>

    val c1 = <!FORBIDDEN_IDENTITY_EQUALS!>fn1 === fn2<!> || <!FORBIDDEN_IDENTITY_EQUALS!>fn1 !== fn2<!>
    val c2 = f1 === fn1 || f1 !== fn1
    val c3 = <!EQUALITY_NOT_APPLICABLE!>b1 === fn1<!> || <!EQUALITY_NOT_APPLICABLE!>b1 !== fn1<!>
}