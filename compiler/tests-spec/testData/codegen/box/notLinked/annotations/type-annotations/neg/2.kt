/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 2
 * DESCRIPTION: Type annotations on supertypes with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

abstract class Foo : @<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>unresolved_reference<!>) Any()

class Bar: Foo()

fun box(): String? {
    val x = Bar()

    if (x == null) return null

    return "OK"
}
