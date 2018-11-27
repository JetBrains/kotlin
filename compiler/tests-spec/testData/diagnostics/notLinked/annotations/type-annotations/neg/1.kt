/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 1
 * DESCRIPTION: Type annotations on return type with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

fun case_1(x: String): @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String {
    return x
}
