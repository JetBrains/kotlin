// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

open class Base {
    protected open fun foo() {}
}

expect open class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    public override fun <!EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>foo<!>() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, override */
