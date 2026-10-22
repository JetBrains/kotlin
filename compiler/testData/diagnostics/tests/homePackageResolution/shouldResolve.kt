// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_EXPRESSION
// LANGUAGE: +HomePackageResolution
// RENDER_DIAGNOSTIC_ARGUMENTS

// FILE: a.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("0; 0; 0")!>package first

open class A

fun A.foo() {}
fun A.bar() {}
fun baz(a: A) { }<!>

// FILE: b.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("3; 0; 2")!>package second

import first.A
import first.foo
import first.bar

class B : A()

fun B.foo() {}
fun baz(b: B) { }

fun checkB(b: B) {
    b.foo()
    b.bar()
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
