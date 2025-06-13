// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

interface I {
    fun foo() {}
}
expect class Foo : I

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo : I by object : I {
    override fun foo() {}
}

/* GENERATED_FIR_TAGS: actual, anonymousObjectExpression, classDeclaration, expect, functionDeclaration,
inheritanceDelegation, interfaceDeclaration, override */
