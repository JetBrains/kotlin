// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: -SupportEffectivelyFinalInExpectActualVisibilityCheck
// MODULE: m1-common
// FILE: common.kt

open class Base {
    internal open fun foo() {}
}
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class Foo : Base {
    override fun <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>foo<!>()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : Base() {
    public actual override fun <!EXPECT_ACTUAL_INCOMPATIBLE_VISIBILITY!>foo<!>() {
    }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, override */
