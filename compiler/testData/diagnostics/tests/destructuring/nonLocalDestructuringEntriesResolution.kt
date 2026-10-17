// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// FIR_DUMP
// ISSUE: KT-90199

// FILE: declarations.kt
data class X(val a: Int, val b: String)

class NoComponents

val <!SYNTAX!>(<!REDECLARATION!>topA<!>, topB)<!> = X(1, "")
val <!REDECLARATION!>topA<!> = ""

val <!SYNTAX!>[topC, topD]<!> = X(1, "")

val <!SYNTAX!>(topE, topF)<!> = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>NoComponents()<!>

var <!SYNTAX!>(topG: Int, topH)<!> = X(1, "")

class C {
    val <!SYNTAX!>(<!REDECLARATION!>a<!>, b: String)<!> = X(1, "")
    val <!REDECLARATION!>a<!> = ""

    val <!SYNTAX!>(c, d)<!> = X(1, "")

    fun use() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
        b.length
        c.inc()
        d.length
    }
}

// FILE: usages.kt
fun use(c: C) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>topA<!>.<!UNRESOLVED_REFERENCE!>length<!>
    topB.length
    topC.inc()
    topG = 1
    c.<!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>.<!UNRESOLVED_REFERENCE!>length<!>
    c.b.length
    c.d.length
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, data, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
