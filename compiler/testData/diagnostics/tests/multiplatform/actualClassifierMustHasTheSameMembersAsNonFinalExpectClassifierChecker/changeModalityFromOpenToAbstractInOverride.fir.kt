// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
interface Base {
    fun foo() {}
}
expect abstract class Foo() : Base


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual abstract class Foo : Base {
    abstract override fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>foo<!>()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, interfaceDeclaration, override,
primaryConstructor */
