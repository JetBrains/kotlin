// LANGUAGE: +ContextReceivers
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun foo() {}

    // Expected: AMBIGUOUS_ACTUALS.
    // But it doesn't work because context receivers are not yet supported in expect actual matcher KT-61447
    context(Int)
    fun <!ACTUAL_MISSING!>foo<!>() {}
}
