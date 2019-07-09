/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 4
 * DESCRIPTION: Type annotations on type arguments for a containing type of return type, with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

class Inv<T>

fun case_1(): Inv<@Ann(unresolved_reference) String> = TODO()

fun box(): String? {
    try {
        val x = case_1()
        if (x == null) return null
    } catch (e: NotImplementedError) {}

    return "OK"
}
