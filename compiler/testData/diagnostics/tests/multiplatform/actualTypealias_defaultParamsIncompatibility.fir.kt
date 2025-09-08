// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(a: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>Foo<!> = FooImpl

class FooImpl {
    fun foo(a: Int = 2) {
    }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, typeAliasDeclaration */
