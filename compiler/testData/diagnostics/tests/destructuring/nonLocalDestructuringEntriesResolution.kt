// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-90199

// FILE: declarations.kt
data class X(val a: Int, val b: String)

class NoComponents

val <!SYNTAX!>(topA, topB)<!> = X(1, "")
val topA = ""

val <!SYNTAX!>[topC, topD]<!> = X(1, "")

val <!SYNTAX!>(topE, topF)<!> = NoComponents()

var <!SYNTAX!>(topG: Int, topH)<!> = X(1, "")

class C {
    val <!SYNTAX!>(a, b: String)<!> = X(1, "")
    val a = ""

    val <!SYNTAX!>(c, d)<!> = X(1, "")

    fun use() {
        a.length
        <!UNRESOLVED_REFERENCE!>b<!>.length
        <!UNRESOLVED_REFERENCE!>c<!>.inc()
        <!UNRESOLVED_REFERENCE!>d<!>.length
    }
}

// FILE: usages.kt
fun use(c: C) {
    topA.length
    <!UNRESOLVED_REFERENCE!>topB<!>.length
    <!UNRESOLVED_REFERENCE!>topC<!>.inc()
    <!UNRESOLVED_REFERENCE!>topG<!> = 1
    c.a.length
    c.<!UNRESOLVED_REFERENCE!>b<!>.length
    c.<!UNRESOLVED_REFERENCE!>d<!>.length
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, data, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
