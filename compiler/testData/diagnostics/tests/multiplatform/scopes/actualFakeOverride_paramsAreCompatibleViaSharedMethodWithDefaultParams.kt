// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
interface Shared {
    fun sharedMethod(withDefaultParam: Int = 2) {}
}

expect class Foo : Shared

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : Shared

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, interfaceDeclaration */
