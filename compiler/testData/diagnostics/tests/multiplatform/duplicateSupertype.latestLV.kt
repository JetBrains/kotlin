// RUN_PIPELINE_TILL: BACKEND
// IGNORE_FIR_DIAGNOSTICS
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-74221

// MODULE: common

interface A {
    open fun foo() {}
}

expect interface B

class C : A, <!SUPERTYPE_APPEARS_TWICE!>B<!> {}

// MODULE: jvm()()(common)

actual typealias B = A
