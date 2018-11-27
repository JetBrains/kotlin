// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 10
 * DESCRIPTION: Type annotations on a lambda type with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

fun case_1(): Any {
    val x: (Int) -> @Ann(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) Unit = {} // OK, no error in IDE and in the compiler

    return x
}

fun case_2(): Any {
    val x: (@Ann(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) Int) -> Unit = { a: Int -> println(a) } // OK, no error in IDE and in the compiler

    return x
}

fun box(): String? {
    val x = case_1()
    val y = case_2()

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
