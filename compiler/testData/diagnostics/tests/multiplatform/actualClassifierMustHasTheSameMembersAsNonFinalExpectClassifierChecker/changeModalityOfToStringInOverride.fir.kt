// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

expect open class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    final override fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>toString<!>() = "Foo"
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, override, stringLiteral */
