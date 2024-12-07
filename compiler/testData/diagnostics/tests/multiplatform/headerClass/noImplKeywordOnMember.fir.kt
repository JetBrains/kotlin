// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun bar(): String
    fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>bas<!>(f: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo {
    fun <!ACTUAL_MISSING!>bar<!>(): String = "bar"
    fun <!ACTUAL_MISSING!>bas<!>(g: Int) {}
}
