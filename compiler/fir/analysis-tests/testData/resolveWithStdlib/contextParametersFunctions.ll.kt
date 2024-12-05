// LL_FIR_DIVERGENCE
// Diverges until Analysis API implements context parameters
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun of1(): A = <!NO_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(b: B) fun of2(): A = <!NO_CONTEXT_ARGUMENT!>contextOf<!><A>()
fun A.of3(): A = <!NO_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(a: A) fun A.of4(): A = <!NO_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(a: A, b: B) fun of5(): A = <!NO_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(a: A) fun of6(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>contextOf<!>()
context(a: A, c: C) fun of7(): A = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>contextOf<!>()
context(a: A) fun of8() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>contextOf<!>()
context(a: A, c: C) fun of9() = <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>contextOf<!>()

fun context1(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>of1<!>() }
fun context2(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>of2<!>() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>of3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>of4<!>() }
fun context5(): A = context(B()) { <!NO_CONTEXT_ARGUMENT!>of5<!>() }
