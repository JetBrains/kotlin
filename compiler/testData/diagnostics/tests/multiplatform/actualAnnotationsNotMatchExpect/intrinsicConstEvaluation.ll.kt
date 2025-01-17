// LL_FIR_DIVERGENCE
// Not a real LL divergence, it's just tiered runners reporting errors from `BACKEND`
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
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
@Ann(MyEnum.FOO.name)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>matching<!>() {}

@Ann(MyEnum.FOO.name)
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>nonMatching<!>() {}
