/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 7
 * DESCRIPTION: Type annotations on a type in an anonymous object expression, with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann

open class TypeToken<T>

val case_1 = object : TypeToken<@Ann(unresolved_reference) String>() {}

interface A

val case_2 = object: @Ann(unresolved_reference) A {}

fun box(): String? {
    val x = case_1
    val y = case_2

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
