// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 7
 * DESCRIPTION: Type annotations on a type in an anonymous object expression, with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
open class TypeToken<T>

val case_1 = object : TypeToken<@Ann(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>unresolved_reference<!>) String>() {}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
interface A

val case_2 = object: @Ann(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>unresolved_reference<!>) A {}
