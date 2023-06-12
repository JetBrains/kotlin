// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 3
 * DESCRIPTION: Type annotations on parameter types with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2, 3
package sometest

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
class Inv<T>

fun foo(i: Inv<@Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String>) {}

// TESTCASE NUMBER: 2
fun test(vararg a: @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Any) {}

// TESTCASE NUMBER: 3
class A<T>(a: @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) T)
