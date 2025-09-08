// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MODULE: m1-common
// FILE: common.kt

open expect class A() {
    open fun foo(): String
}

expect class B() : A

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun test1() = B().foo()

fun test2() = with(B()) {
    foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, expect, functionDeclaration, lambdaLiteral, primaryConstructor */
