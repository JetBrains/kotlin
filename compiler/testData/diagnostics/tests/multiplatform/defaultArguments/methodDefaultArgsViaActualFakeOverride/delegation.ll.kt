// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(param: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface Base {
    fun foo(p: Int)
}

object BaseImpl : Base {
    override fun foo(p: Int) {}
}

actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>Foo<!> : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base by BaseImpl<!>

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, inheritanceDelegation, integerLiteral,
interfaceDeclaration, objectDeclaration, override */
