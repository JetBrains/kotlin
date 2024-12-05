// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class A
class B: A()

class C

context(a: A) fun of1(): A = contextOf<A>()
context(b: B) fun of2(): A = contextOf<A>()
fun A.of3(): A = contextOf<A>()
context(a: A) fun A.of4(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(a: A, b: B) fun of5(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT!>contextOf<!><A>()
context(a: A) fun of6(): A = contextOf()
context(a: A, c: C) fun of7(): A = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()
context(a: A) fun of8() = contextOf()
context(a: A, c: C) fun of9() = <!AMBIGUOUS_CONTEXT_ARGUMENT, CANNOT_INFER_PARAMETER_TYPE!>contextOf<!>()

fun context1(): A = context(B()) { of1() }
fun context2(): A = context(B()) { of2() }
fun context3(): A = context(B()) { <!UNRESOLVED_REFERENCE!>of3<!>() }
fun context4(): A = context(B()) { <!UNRESOLVED_REFERENCE!>of4<!>() }
fun context5(): A = context(B()) { of5() }
