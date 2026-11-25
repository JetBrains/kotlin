// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ISSUE: KT-90199

// FILE: declarations.kt
data class X(val a: Int, val b: String)

class NoComponents

val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(<!REDECLARATION!>topA<!>, topB)<!> = X(1, "")
val <!REDECLARATION!>topA<!> = ""

val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>[topC, topD]<!> = X(1, "")

val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(topE, topF)<!> = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>NoComponents()<!>

var <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(topG: Int, topH)<!> = X(1, "")

class C {
    val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(<!REDECLARATION!>a<!>, b: String)<!> = X(1, "")
    val <!REDECLARATION!>a<!> = ""

    val <!NON_LOCAL_DESTRUCTURING_DECLARATION!>(c, d)<!> = X(1, "")

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
