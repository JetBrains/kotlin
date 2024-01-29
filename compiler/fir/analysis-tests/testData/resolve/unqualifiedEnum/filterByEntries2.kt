// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

enum class A {
    P, Q
}
enum class B {
    P, Q
}

// may take A or B
fun foo(a: A): Int = 1
fun foo(b: B): Int = 2

// takes only A
fun bar(a: A): Int = 3

val result = foo(<!UNRESOLVED_REFERENCE!>P<!>) + bar(Q)

fun <T> generic1(x: T): Int = 4
fun <T> generic2(x: T, y: T): Int = 5

val result1a = generic1(A.P)
val result1b = <!CANNOT_INFER_PARAMETER_TYPE!>generic1<!>(<!UNRESOLVED_REFERENCE!>P<!>)

val result2a = generic2(A.P, A.Q)
val result2b = generic2(A.P, <!UNRESOLVED_REFERENCE!>Q<!>)
