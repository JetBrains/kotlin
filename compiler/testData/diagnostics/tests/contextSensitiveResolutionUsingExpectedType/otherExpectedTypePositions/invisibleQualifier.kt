// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86093

// FILE: privates.kt
package pkg

private val A: Int = 42
private object B
class C {
    private companion object
}
private object Aliased

// FILE: main.kt
import pkg.*
import pkg.<!INVISIBLE_REFERENCE!>Aliased<!> as X

enum class E {
    A, B, C, X
}

fun test() {
    val p1: E = A
    val p2: E = <!INITIALIZER_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>
    val p3: E = <!INITIALIZER_TYPE_MISMATCH, INVISIBLE_REFERENCE!>C<!>
    val p4: E = <!INITIALIZER_TYPE_MISMATCH, INVISIBLE_REFERENCE!>X<!>
}

fun negative() {
    val n1: Int = <!INITIALIZER_TYPE_MISMATCH, INVISIBLE_REFERENCE!>B<!>
    val n2: Any = <!INVISIBLE_REFERENCE!>C<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, localProperty, objectDeclaration, propertyDeclaration */
