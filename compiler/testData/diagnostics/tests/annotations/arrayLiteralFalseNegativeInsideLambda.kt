// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-71708

inline fun build(action: () -> Unit) {}

fun foo(x: Int) = build {
    if (x == 1) <!UNSUPPORTED!>[1]<!>
}
