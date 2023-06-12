// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 6
 * DESCRIPTION: Type annotations inside type check and cast expression with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
fun case_1(a: Any) {
    if (a is @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String) return
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2(a: Any) {
    a as @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String // OK, no error in IDE and in the compiler
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 */
fun case_3_1(a: Any) {}

fun case_3_2(a: Any) {
    case_3_1(a as @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String) // OK, no error in IDE and in the compiler
}

// TESTCASE NUMBER: 4
fun case_4(a: Any) {
    val x = a as @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) String // ERROR, has error in IDE and in the compiler
}
