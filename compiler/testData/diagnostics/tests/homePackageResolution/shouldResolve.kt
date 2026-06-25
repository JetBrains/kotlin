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
<!HOME_PACKAGE_WOULD_RESOLVE_THIS!>import first.foo<!>
<!HOME_PACKAGE_WOULD_RESOLVE_THIS!>import first.bar<!>

class B : A()

fun B.foo() {}
fun baz(b: B) { }

fun checkB(b: B) {
    b.foo()
    b.bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
