// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing errorg: Ignoring an implementation of the method `void C.foo()` because it has multiple definitions
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

interface A {
    open fun foo() {}
}

expect interface B

class C : A, <!SUPERTYPE_APPEARS_TWICE{JVM}!>B<!> {}

// MODULE: jvm()()(common)

actual typealias B = A
