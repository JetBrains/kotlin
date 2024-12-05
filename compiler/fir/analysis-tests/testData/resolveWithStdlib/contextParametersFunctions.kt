// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun usage1(): A = fromContext<A>()
context(b: B) fun usage2(): A = fromContext<A>()
fun A.usage3(): A = fromContext<A>()
context(a: A) fun A.usage4(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(a: A, b: B) fun usage5(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>fromContext<!><A>()
context(a: A) fun usage6(): A = fromContext()
context(a: A, c: C) fun usage7(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>fromContext<!>()
context(a: A) fun usage8() = fromContext()
context(a: A, c: C) fun usage9() = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>fromContext<!>()

fun context1(): A = context(B()) { usage1() }
fun context2(): A = context(B()) { usage2() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage4<!>() }
fun context5(): A = context(B()) { usage5() }
fun context6(): A = context(B()) { usage6() }
fun context7(): A = context(B()) { <!NO_CONTEXT_ARGUMENT("C")!>usage7<!>() }
fun context8(): A = context(B()) { usage8() }
fun context9(): A = context(B()) { <!NO_CONTEXT_ARGUMENT("C")!>usage9<!>() }
