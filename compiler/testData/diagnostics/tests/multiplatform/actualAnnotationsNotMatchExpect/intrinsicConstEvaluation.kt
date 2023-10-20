// !LANGUAGE: +IntrinsicConstEvaluation
// MODULE: m1-common
// FILE: common.kt
enum class MyEnum {
    FOO
}

annotation class Ann(val p: String)

@Ann("FOO")
expect fun matching()

@Ann("not FOO")
expect fun nonMatching()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum.FOO.name<!>)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>matching<!>() {}

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum.FOO.name<!>)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>nonMatching<!>() {}
