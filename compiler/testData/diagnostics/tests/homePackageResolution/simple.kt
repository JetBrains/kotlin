// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
// FIR_DUMP
// LANGUAGE: +HomePackageResolution
// RENDER_DIAGNOSTIC_ARGUMENTS

// FILE: a.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("0; 0; 0")!>package first

open class A

fun A.foo() {}
fun A.bar() {}
fun baz(a: A) { }<!>

// FILE: b.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("1; 0; 0")!>package second

import first.A

class B : A()

fun B.foo() {}
fun baz(b: B) { }

fun checkB(b: B) {
    b.foo()
    b.bar()
}<!>

// FILE: c.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("2; 0; 0")!>package other

import first.A
import second.B

fun checkC(a: A, b: B) {
    a.foo()
    a.bar()
    <!UNRESOLVED_REFERENCE("baz")!>baz<!>(a)

    b.foo()
    b.bar()
    <!UNRESOLVED_REFERENCE("baz")!>baz<!>(b)
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
