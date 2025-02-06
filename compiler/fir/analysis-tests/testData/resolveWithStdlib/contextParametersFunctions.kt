// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun implicit1(): A = implicit<A>()
context(b: B) fun implicit2(): A = implicit<A>()
fun A.implicit3(): A = implicit<A>()
context(a: A) fun A.implicit4(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>implicit<!><A>()
context(a: A, b: B) fun implicit5(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>implicit<!><A>()
context(a: A) fun implicit6(): A = implicit()
context(a: A, c: C) fun implicit7(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>implicit<!>()
context(a: A) fun implicit8() = implicit()
context(a: A, c: C) fun implicit9() = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>implicit<!>()

fun context1(): A = context(B()) { implicit1() }
fun context2(): A = context(B()) { implicit2() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>implicit3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>implicit4<!>() }
fun context5(): A = context(B()) { implicit5() }
