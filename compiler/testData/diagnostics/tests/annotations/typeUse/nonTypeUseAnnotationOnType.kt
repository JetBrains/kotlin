// FIR_IDENTICAL
// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-28449

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Ann

abstract class Foo : <!WRONG_ANNOTATION_TARGET!>@Ann<!> Any()

abstract class Bar<T : <!WRONG_ANNOTATION_TARGET!>@Ann<!> Any>

fun test_1(a: Any) {
    if (a is <!WRONG_ANNOTATION_TARGET!>@Ann<!> String) return
}

open class TypeToken<T>
val test_2 = object : TypeToken<<!WRONG_ANNOTATION_TARGET!>@Ann<!> String>() {}

fun test_3(a: Any) {
    a as <!WRONG_ANNOTATION_TARGET!>@Ann<!> Int
}

fun <T> test_4() where T : <!WRONG_ANNOTATION_TARGET!>@Ann<!> Any, T : <!WRONG_ANNOTATION_TARGET!>@Ann<!> CharSequence {}
