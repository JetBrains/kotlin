// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class Ann

interface I

expect class Foo: @Ann I

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
typealias ITypealias = I

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> class Foo : ITypealias
