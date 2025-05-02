// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface Base {
    fun foo()
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base {
    final override fun <!EXPECT_ACTUAL_INCOMPATIBILITY_MODALITY!>foo<!>() {}
}
