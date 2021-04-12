// !DIAGNOSTICS: -UNUSED_PARAMETER

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
@Target(AnnotationTarget.TYPE)
annotation class Ann

var <T> T.test
    get() = 11
    set(value: @Ann(<!TOO_MANY_ARGUMENTS, TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>unresolved_reference<!>) Int) {}
