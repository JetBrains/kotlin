/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 6
 * DESCRIPTION: Type annotations inside type check and cast expression with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

fun case_1(a: Any): Int? {
    return if (a is @Ann(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) String) 10 else null
}

fun case_2(a: Any): Any {
    return a as @<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) String // OK, no error in IDE and in the compiler
}

fun case_3_1(a: Any) = 10

fun case_3(a: Any): Any {
    return case_3_1(a as @<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) String) // OK, no error in IDE and in the compiler
}

fun box(): String? {
    val x = case_1(".")
    val y = case_2(".")
    val z = case_3(".")

    if (x == null) return null
    if (y == null) return null
    if (z == null) return null

    return "OK"
}
