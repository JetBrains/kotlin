// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// MODULE: m1-common
// FILE: common.kt
enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MyEnum<!> {
    FOO
}

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val p: String)

<!CONFLICTING_OVERLOADS!>@Ann("FOO")
expect fun matching()<!>

<!CONFLICTING_OVERLOADS!>@Ann("not FOO")
expect fun nonMatching()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum.FOO.name<!>)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>matching<!>() {}

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>MyEnum.FOO.name<!>)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>nonMatching<!>() {}
