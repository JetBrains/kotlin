// !DIAGNOSTICS: -UNUSED_VARIABLE

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 10
 * DESCRIPTION: Type annotations on a lambda type with unresolved reference in parameters.
 * ISSUES: KT-28424
 */

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
fun case_1() {
    val x: (Int) -> @Ann(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) Unit = {} // OK, no error in IDE and in the compiler
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2() {
    val x: (@Ann(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) Int) -> Unit = { a: Int -> println(a) } // OK, no error in IDE and in the compiler
}

// TESTCASE NUMBER: 3
fun case_3() {
    val x: (@Ann(<!UNRESOLVED_REFERENCE!>unresolved_reference<!>) Int) -> Unit = { a -> println(a) } // ERROR (if argument type isn't specified explicitly)
}
