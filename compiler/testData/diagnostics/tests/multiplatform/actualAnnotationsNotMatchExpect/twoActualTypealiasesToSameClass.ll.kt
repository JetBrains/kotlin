// LL_FIR_DIVERGENCE
// Not a real LL divergence, it's just tiered runners reporting errors from `BACKEND`
// LL_FIR_DIVERGENCE
// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class WithAnn {
    @Ann
    fun foo()
}

expect class WithoutAnn {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class Impl {
    fun foo() {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>WithAnn<!> = Impl
actual typealias WithoutAnn = Impl
