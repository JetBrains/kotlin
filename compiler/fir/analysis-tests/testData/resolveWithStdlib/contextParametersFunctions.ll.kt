// LL_FIR_DIVERGENCE
// Diverges until Analysis API implements context parameters
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun implicit1(): A = <!NO_CONTEXT_ARGUMENT!>implicit<!><A>()
context(b: B) fun implicit2(): A = <!NO_CONTEXT_ARGUMENT!>implicit<!><A>()
fun A.implicit3(): A = <!NO_CONTEXT_ARGUMENT!>implicit<!><A>()
context(a: A) fun A.implicit4(): A = <!NO_CONTEXT_ARGUMENT!>implicit<!><A>()
context(a: A, b: B) fun implicit5(): A = <!NO_CONTEXT_ARGUMENT!>implicit<!><A>()
context(a: A) fun implicit6(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>implicit<!>()
context(a: A, c: C) fun implicit7(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>implicit<!>()
context(a: A) fun implicit8() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>implicit<!>()
context(a: A, c: C) fun implicit9() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>implicit<!>()

fun context1(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>implicit1<!>() }
fun context2(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>implicit2<!>() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>implicit3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>implicit4<!>() }
fun context5(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>implicit5<!>() }
