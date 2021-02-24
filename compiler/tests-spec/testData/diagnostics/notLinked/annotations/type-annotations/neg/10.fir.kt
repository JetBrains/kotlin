// !DIAGNOSTICS: -UNUSED_VARIABLE

// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 */
fun case_1() {
    val x: (Int) -> @Ann(unresolved_reference) Unit = {} // OK, no error in IDE and in the compiler
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 */
fun case_2() {
    val x: (@Ann(unresolved_reference) Int) -> Unit = { a: Int -> println(a) } // OK, no error in IDE and in the compiler
}

// TESTCASE NUMBER: 3
fun case_3() {
    val x: (@Ann(unresolved_reference) Int) -> Unit = { a -> println(a) } // ERROR (if argument type isn't specified explicitly)
}
