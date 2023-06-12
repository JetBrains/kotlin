// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 4
 * DESCRIPTION: Type annotations on type arguments for a containing type of return type, with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1
package sometest

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

class Inv<T>

fun case_1(): Inv<@Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String> = TODO()
