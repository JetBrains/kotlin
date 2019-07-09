/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 8
 * DESCRIPTION: Type annotations on a receiver type (for an extension property only), with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

val <T> @Ann(unresolved_reference) T.test // OK, error only in IDE but not in the compiler
    get() = 10

val @Ann(unresolved_reference) Int.test
    get() = 10

fun box(): String? {
    val x = 10.test
    val y = '.'.test

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
