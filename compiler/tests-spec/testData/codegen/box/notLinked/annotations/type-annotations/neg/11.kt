/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 11
 * DESCRIPTION: Type annotations with invalid target.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28449
 */

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Ann(val x: Int)

class Foo : @Ann(10) Any()

class Bar<T : @Ann(10) Any>

fun case_3(a: Any): Int? {
    return if (a is @Ann(10) String) 10 else null
}

open class TypeToken<T>

val case_4 = object : TypeToken<@Ann(10) String>() {}

fun case_5(a: Any): Any {
    a as @Ann(10) Int

    return a
}

fun box(): String? {
    val x1 = Foo()
    val x2 = Bar<Int>()
    val x3 = case_3(".")
    val x4 = case_4
    val x5 = case_5(10)

    if (x1 == null) return null
    if (x2 == null) return null
    if (x3 == null) return null
    if (x4 == null) return null
    if (x5 == null) return null

    return "OK"
}
