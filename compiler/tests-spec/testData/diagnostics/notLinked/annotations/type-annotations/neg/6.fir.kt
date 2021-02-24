// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
fun case_1(a: Any) {
    if (a is @Ann(unresolved_reference) String) return
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2(a: Any) {
    a as @Ann(unresolved_reference) String // OK, no error in IDE and in the compiler
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 */
fun case_3_1(a: Any) {}

fun case_3_2(a: Any) {
    case_3_1(a as @Ann(unresolved_reference) String) // OK, no error in IDE and in the compiler
}

// TESTCASE NUMBER: 4
fun case_4(a: Any) {
    val x = a as @Ann(unresolved_reference) String // ERROR, has error in IDE and in the compiler
}
