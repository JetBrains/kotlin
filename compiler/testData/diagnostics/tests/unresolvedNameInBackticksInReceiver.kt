// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// ISSUE: KT-60597

fun test() {
    <!UNRESOLVED_REFERENCE!>`java.lang.Short.TYPE`<!>.getConstructor(TODO())
}
