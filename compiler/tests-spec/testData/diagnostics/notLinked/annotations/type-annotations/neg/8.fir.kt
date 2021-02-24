// TESTCASE NUMBER: 1, 2
@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
val <T> @Ann(unresolved_reference) T.test // OK, error only in IDE but not in the compiler
    get() = 10

// TESTCASE NUMBER: 2
val @Ann(unresolved_reference) Int.test
    get() = 10
