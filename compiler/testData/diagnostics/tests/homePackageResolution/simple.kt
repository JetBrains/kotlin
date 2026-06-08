// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
// FIR_DUMP

// FILE: a.kt

package first

open class A

fun A.foo() {}
fun A.bar() {}
fun baz(a: A) { }

// FILE: b.kt

package second

import first.A

class B : A()

fun B.foo() {}
fun baz(b: B) { }

fun checkB(b: B) {
    b.foo()
    b.bar()
}

// FILE: c.kt

package other

import first.A
import second.B

fun checkC(a: A, b: B) {
    a.foo()
    a.bar()
    <!UNRESOLVED_REFERENCE!>baz<!>(a)

    b.foo()
    b.bar()
    <!UNRESOLVED_REFERENCE!>baz<!>(b)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
