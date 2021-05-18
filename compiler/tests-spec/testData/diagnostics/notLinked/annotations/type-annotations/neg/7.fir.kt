// !WITH_NEW_INFERENCE

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
open class TypeToken<T>

val case_1 = object : TypeToken<@Ann(unresolved_reference) String>() {}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
interface A

val case_2 = object: @Ann(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>unresolved_reference<!>) A {}
