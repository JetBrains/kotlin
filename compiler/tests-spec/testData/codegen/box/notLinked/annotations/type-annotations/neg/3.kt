/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 3
 * DESCRIPTION: Type annotations on parameter types with unresolved reference in parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28424
 */

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

class Inv<T>

fun foo(i: Inv<@Ann(unresolved_reference) String>) {}

fun bar(vararg a: @Ann(unresolved_reference) Any) {}

class A<T>(a: @Ann(unresolved_reference) T)

fun box(): String? {
    val x = foo(Inv<String>())
    val y = bar(1, 2, 3)
    val z = A<Int>(10)

    if (x == null) return null
    if (y == null) return null
    if (z == null) return null

    return "OK"
}
