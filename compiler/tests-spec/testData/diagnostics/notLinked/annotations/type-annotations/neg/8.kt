/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 8
 * DESCRIPTION: Type annotations on a receiver type (for an extension property only), with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
val <T> @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) T.test // OK, error only in IDE but not in the compiler
    get() = 10

// TESTCASE NUMBER: 2
val @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Int.test
    get() = 10
