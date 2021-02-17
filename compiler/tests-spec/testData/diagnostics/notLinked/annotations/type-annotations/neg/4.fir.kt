// TESTCASE NUMBER: 1
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

fun case_1(): Inv<@Ann(unresolved_reference) String> = TODO()
