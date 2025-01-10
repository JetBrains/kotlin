// RUN_PIPELINE_TILL: FIR2IR
// IGNORE_FIR_DIAGNOSTICS
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing errorg: Ignoring an implementation of the method `void C.foo()` because it has multiple definitions
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-74221

// MODULE: common

interface A {
    open fun foo() {}
}

expect interface B

class C : A, B {}

// MODULE: jvm()()(common)

actual typealias B = A
