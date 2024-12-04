// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-71117
open class A<H: Function2<*, *, <!REDUNDANT_PROJECTION!>out<!> Function0<String>>>(val f1: H, val f2: IntArray) {}

class B: A<Function2<*, *, Function0<String>>>({p1: List<*>, p2: String -> {""}}, IntArray(0)) {}
