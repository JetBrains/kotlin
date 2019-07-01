// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewInference
// Issue: KT-30734

class Sample {
    fun foo(): Boolean = true
}

fun test(ls: Sample?) {
    val filter: () -> Boolean = if (ls == null) {
        { false }
    } else {
        { <!DEBUG_INFO_SMARTCAST!>ls<!>.foo() } // OK in OI, error in NI
    }
}