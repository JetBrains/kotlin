// !LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-28449

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Ann

abstract class Foo : @Ann Any()

abstract class Bar<T : @Ann Any>

fun test_1(a: Any) {
    if (a is @Ann String) return
}

open class TypeToken<T>
val test_2 = object : TypeToken<@Ann String>() {}

fun test_3(a: Any) {
    a as @Ann Int
}

fun <T> test_4() where T : @Ann Any, T : @Ann CharSequence {}
