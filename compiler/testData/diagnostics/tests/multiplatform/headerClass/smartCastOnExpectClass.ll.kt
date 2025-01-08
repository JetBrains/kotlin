// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect class Foo { // also, it's important that Foo doesn't override equals
    fun foo()
}

fun check(x1: Foo, x: Any) {
    if (x1 == x) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

// MODULE: m1-jvm()()(m1-common)
