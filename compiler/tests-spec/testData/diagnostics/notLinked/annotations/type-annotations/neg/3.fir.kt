// !DIAGNOSTICS: -UNUSED_PARAMETER

// TESTCASE NUMBER: 1, 2, 3
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
fun foo(i: Inv<@Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String>) {}

// TESTCASE NUMBER: 2
fun test(vararg a: @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Any) {}

// TESTCASE NUMBER: 3
class A<T>(a: @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) T)
