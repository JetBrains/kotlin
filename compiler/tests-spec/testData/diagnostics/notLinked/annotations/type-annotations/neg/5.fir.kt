/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 5
 * DESCRIPTION: Type annotations on upper bounds with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
abstract class Bar<T : @Ann(unresolved_reference) Any>

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
class B<T> where @Ann(unresolved_reference) T : Number
