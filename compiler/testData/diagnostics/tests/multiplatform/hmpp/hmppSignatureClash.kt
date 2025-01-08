// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class A<!> {
    fun foo(x: String): String
}

// MODULE: intermediate()()(common)
expect class B

actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class A<!> {
    actual fun foo(x: B) = "a"
}

// MODULE: main()()(intermediate)
actual typealias B = String
