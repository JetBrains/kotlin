// LL_FIR_DIVERGENCE
// Diverges until Analysis API implements context parameters
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun usage1(): A = <!NO_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(b: B) fun usage2(): A = <!NO_CONTEXT_ARGUMENT!>fromContext<!><A>()
fun A.usage3(): A = <!NO_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(a: A) fun A.usage4(): A = <!NO_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(a: A, b: B) fun usage5(): A = <!NO_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(a: A) fun usage6(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>fromContext<!>()
context(a: A, c: C) fun usage7(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>fromContext<!>()
context(a: A) fun usage8() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>fromContext<!>()
context(a: A, c: C) fun usage9() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>fromContext<!>()

fun context1(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>usage1<!>() }
fun context2(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>usage2<!>() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage4<!>() }
fun context5(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>usage5<!>() }
fun context6(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>usage6<!>() }
fun context7(): A = context(B()) { <!NO_CONTEXT_ARGUMENT("A")!>usage7<!>() }
fun context8(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>usage8<!>() }
fun context9(): A = context(B()) { <!NO_CONTEXT_ARGUMENT("A")!>usage9<!>() }
