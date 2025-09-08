// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    class <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>Bar<!>() {
        fun <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>foo<!>(p: Int = 1)
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base {
    fun foo(p: Int) {}
}

actual class Foo {
    actual class Bar : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base()<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, nestedClass,
primaryConstructor */
