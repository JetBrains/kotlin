// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class Foo { // also, it's important that Foo doesn't override equals
    fun foo()
}

fun check(x1: Foo, x: Any) {
    if (x1 == x) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
