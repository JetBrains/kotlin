// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
open class A(p1: String, p2: String, p3: String, p4: String, p5: String)

class B : A {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor(s: String)<!>
}
