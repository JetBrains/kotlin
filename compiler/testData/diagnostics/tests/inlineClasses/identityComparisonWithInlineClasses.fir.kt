// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

inline class Foo(val x: Int)
inline class Bar(val y: String)

fun test(f1: Foo, f2: Foo, b1: Bar, fn1: Foo?, fn2: Foo?) {
    val a1 = f1 === f2 || f1 !== f2
    val a2 = f1 === f1
    val a3 = <!EQUALITY_NOT_APPLICABLE!>f1 === b1<!> || <!EQUALITY_NOT_APPLICABLE!>f1 !== b1<!>

    val c1 = fn1 === fn2 || fn1 !== fn2
    val c2 = f1 === fn1 || f1 !== fn1
    val c3 = <!EQUALITY_NOT_APPLICABLE!>b1 === fn1<!> || <!EQUALITY_NOT_APPLICABLE!>b1 !== fn1<!>

    val any = Any()

    val d1 = any === f1 || any !== f1
    val d2 = f1 === any || f1 !== any
    val d3 = any === fn1 || any !== fn1
    val d4 = fn1 === any || fn1 !== any
}
