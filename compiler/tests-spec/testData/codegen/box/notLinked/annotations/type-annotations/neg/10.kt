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
    val x: (Int) -> @Ann(unresolved_reference) Unit = {}

    return x
}

fun case_2(): Any {
    val x: (@Ann(unresolved_reference) Int) -> Unit = { a: Int -> println(a) }

    return x
}

fun box(): String? {
    val x = case_1()
    val y = case_2()

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
