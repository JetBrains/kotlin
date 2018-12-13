/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 5
 * DESCRIPTION: Type annotations on upper bounds with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

abstract class Bar<T : @Ann(unresolved_reference) Any>

class Foo<T : Any> : Bar<T>()

class B<T> where @Ann(unresolved_reference) T : Number

fun box(): String? {
    val x = Foo<Int>()
    val y = B<Float>()

    if (x == null) return null
    if (y == null) return null

    return "OK"
}
