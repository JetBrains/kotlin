// TESTCASE NUMBER: 1
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

fun case_1(x: String): @Ann(unresolved_reference) String {
    return x
}
