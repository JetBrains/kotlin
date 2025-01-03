// RUN_PIPELINE_TILL: FIR2IR
// IGNORE_DEXING
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

interface A {
    open fun foo() {}
}

expect interface B

class C : A, B {}

// MODULE: jvm()()(common)

actual typealias B = A
