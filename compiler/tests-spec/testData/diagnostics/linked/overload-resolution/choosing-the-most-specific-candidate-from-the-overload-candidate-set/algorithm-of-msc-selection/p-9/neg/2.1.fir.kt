// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class A : B, C
interface B
interface C
fun foo(x: B) {} //(1)
fun foo(y: C, z: String = "foo") {} //2
fun case1() {
    <!AMBIGUITY!>foo<!>(A()) //OVERLOAD_RESOLUTION_AMBIGUITY
}
