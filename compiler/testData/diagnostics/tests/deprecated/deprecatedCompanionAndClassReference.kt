// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-54209

class A {
    @Deprecated("Deprecated companion")
    companion object
}


fun test() {
    A::class
    A.<!DEPRECATION!>Companion<!>::class
}
