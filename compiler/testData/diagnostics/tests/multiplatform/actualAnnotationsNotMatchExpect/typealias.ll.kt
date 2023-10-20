// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class Ann

@Ann
expect class KtTypealiasNotMatch

@Ann
expect class AnnotationsNotConsideredOnTypealias

annotation class ComplexAnn(val s: String)

expect class MethodsInsideTypealias {
    @Ann
    fun foo()
}

expect class ValueInsideTypealias {
    @Ann
    val value: String
}

expect class ConstructorInsideTypealias @Ann constructor()

expect class MethodWithComplexAnnInsideTypealias {
    @ComplexAnn("1" + "2")
    fun withComplexAnn()
}

expect class InnerClassInsideTypealias {
    class Foo {
        @Ann
        fun foo()
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class KtTypealiasNotMatchImpl

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>KtTypealiasNotMatch<!> = KtTypealiasNotMatchImpl

class AnnotationsNotConsideredOnTypealiasImpl

@Ann
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>AnnotationsNotConsideredOnTypealias<!> = AnnotationsNotConsideredOnTypealiasImpl

class MethodsInsideTypealiasImpl {
    fun foo() {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MethodsInsideTypealias<!> = MethodsInsideTypealiasImpl

class ValueInsideTypealiasImpl {
    val value: String = ""
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ValueInsideTypealias<!> = ValueInsideTypealiasImpl

class ConstructorInsideTypealiasImpl

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ConstructorInsideTypealias<!> = ConstructorInsideTypealiasImpl

class MethodWithComplexAnnInsideTypealiasImpl {
    @ComplexAnn("13")
    fun withComplexAnn() {}
}

actual typealias MethodWithComplexAnnInsideTypealias = MethodWithComplexAnnInsideTypealiasImpl

class InnerClassInsideTypealiasImpl {
    class Foo {
        fun foo() {}
    }
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>InnerClassInsideTypealias<!> = InnerClassInsideTypealiasImpl
