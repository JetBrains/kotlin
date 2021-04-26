/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

abstract class Foo : @Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Any()
