// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// ISSUE: KT-61447
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun <!AMBIGUOUS_ACTUALS{JVM}!>foo<!>()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun foo() {}

    context(Int)
    fun <!ACTUAL_MISSING!>foo<!>() {}
}
