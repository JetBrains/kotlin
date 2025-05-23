// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-41934
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// DUMP_CONSTRAINTS: FIXATION

fun <T, VR : T> foo(x: T, fn: (VR?, T) -> Unit) {}

fun takeInt(x: Int) {}

fun main(x: Int) {
    foo(x) { prev: Int?, new -> takeInt(new) } // `new` is `Int` in OI, `Int?` in NI
}
