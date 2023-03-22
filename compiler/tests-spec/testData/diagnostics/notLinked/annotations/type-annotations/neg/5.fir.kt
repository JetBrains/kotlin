// IGNORE_REVERSED_RESOLVE
// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
abstract class Bar<T : @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Any>

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
class B<T> where <!ANNOTATION_IN_WHERE_CLAUSE_ERROR, WRONG_ANNOTATION_TARGET!>@Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>)<!> T : Number
