/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 9
 * DESCRIPTION: Type annotations on a setter argument type with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann

var <T> T.test
    get() = 11
    set(value: @Ann(unresolved_reference) Int) {}

fun box(): String? {
    val x = 10.test
    10.test = 11
    val y = 10.test

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
