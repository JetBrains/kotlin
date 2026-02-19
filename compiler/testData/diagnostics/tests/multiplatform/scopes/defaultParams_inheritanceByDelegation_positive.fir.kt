// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class E {
    fun f(x: Int): Int
}

expect class E2 {
    fun f(x: Int): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface I {
    fun f(x: Int = 5): Int = x
}

actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>E<!>(i: I) : I by i

actual class E2(i: I) : I by i {
    actual override fun f<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>(x: Int)<!> = x
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, inheritanceDelegation, integerLiteral,
interfaceDeclaration, override, primaryConstructor */
