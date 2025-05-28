// LL_FIR_DIVERGENCE
// KT-77874
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters


open class A
class B: A()

class C


// 'contextOf'

// green code
context(a: A) fun usage1() { contextOf<A>() }
context(a: A) fun usage2(): A = contextOf<A>()
context(a: A) fun usage3() = contextOf<A>()

// green code
fun A.usage4() { contextOf<A>() }
fun A.usage5(): A = contextOf<A>()
fun A.usage6() = contextOf<A>()

// green code
context(b: B) fun usage7() { contextOf<A>() }
context(b: B) fun usage8(): A = contextOf<A>()
context(b: B) fun usage9(): A = contextOf<B>()

// red code (ambiguous context argument)
context(a: A) fun A.usage10() { <!AMBIGUOUS_CONTEXT_ARGUMENT("<unused var>@A")!>contextOf<!><A>() }
context(a: A) fun A.usage11(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT("<unused var>@A")!>contextOf<!><A>()
context(a: A, b: B) fun usage12() { <!AMBIGUOUS_CONTEXT_ARGUMENT("<unused var>@A")!>contextOf<!><A>() }
context(a: A, b: B) fun usage13(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT("<unused var>@A")!>contextOf<!><A>()

// green code
context(a: A, c: C) fun usage14() { contextOf<A>() }
context(a: A, c: C) fun usage15(): A = contextOf<A>()

// red code (lack of type inference via context argument)
context(a: A) fun usage16() { <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>() }
context(a: A) fun usage17() = <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
fun A.usage18() { <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>() }
fun A.usage19() = <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(a: A) fun A.usage20() { <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>() }
context(a: A, b: B) fun usage21() { <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>() }
context(a: A, c: C) fun usage22() { <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>() }

// red code (lack of type inference via return type)
context(a: A) fun usage23(): A = <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
fun A.usage24(): A = <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(b: B) fun usage25(): A = <!CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(a: A) fun A.usage26(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(a: A, b: B) fun usage27(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(a: A, c: C) fun usage28(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()


// 'context' w/ 1 context parameter

// green code
fun context1() { context(B()) { usage1() } }
fun context2(): A = context(B()) { usage2() }
fun context3(): A = context(B()) { usage3() }

// red code (context parameters cannot be used as extension receivers)
fun context4() { <!CANNOT_INFER_PARAMETER_TYPE!>context<!>(B()) { <!UNRESOLVED_REFERENCE!>usage4<!>() } }
fun context5(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage5<!>() }
fun context6(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage6<!>() }

// green code
fun context7() { context(B()) { usage7() } }
fun context8(): A = context(B()) { usage8() }
fun context9(): A = context(B()) { usage9() }

// green code
fun context10v1() { context(B()) { with(B()) { usage10() } } }
fun context11v1(): A = context(B()) { with(B()) { usage11() } }

// red code (context parameters cannot be used as extension receivers)
fun context10v2() { <!CANNOT_INFER_PARAMETER_TYPE!>context<!>(B()) { <!UNRESOLVED_REFERENCE!>usage10<!>() } }
fun context11v2(): A = context(B()) { <!UNRESOLVED_REFERENCE!>usage11<!>() }

// green code
fun context12() { context(B()) { usage12() } }
fun context13(): A = context(B()) { usage13() }

// green code
fun context14v1() { context(B()) { context(C()) { usage14() } } }
fun context15v1(): A = context(B()) { context(C()) { usage15() } }

// red code (no context argument)
fun context14v2() { context(B()) { <!NO_CONTEXT_ARGUMENT("c: C")!>usage14<!>() } }
fun context15v2(): A = context(B()) { <!NO_CONTEXT_ARGUMENT("c: C")!>usage15<!>() }
