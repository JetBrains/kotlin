/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: annotations, type-annotations
 * NUMBER: 11
 * DESCRIPTION: Type annotations with invalid target.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28449
 */

// TESTCASE NUMBER: 1, 2, 3, 4, 5
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Ann(val x: Int)

// TESTCASE NUMBER: 1
abstract class Foo : @<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(10) Any()

// TESTCASE NUMBER: 2
abstract class Bar<T : @Ann(10) Any>

// TESTCASE NUMBER: 3
fun case_3(a: Any) {
    if (a is @Ann(10) String) return
}

// TESTCASE NUMBER: 4
open class TypeToken<T>

val case_4 = object : TypeToken<@<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(10) String>() {}

// TESTCASE NUMBER: 5
fun case_5(a: Any) {
    a as @<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!>(10) Int
}
