// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
// LANGUAGE: +HomePackageResolution

// FILE: a.kt

package first

open class A

fun A.foo() {}
fun A.bar() {}
fun baz(a: A) { }

// FILE: b.kt

package second

import first.A
import first.foo
import first.bar

class B : A()

fun B.foo() {}
fun baz(b: B) { }

fun checkB(b: B) {
    b.<!HOME_PACKAGE_WOULD_RESOLVE_THIS!>foo<!>()
    b.<!HOME_PACKAGE_WOULD_RESOLVE_THIS!>bar<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
