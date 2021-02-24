// !WITH_NEW_INFERENCE
// ISSUE: KT-29767

fun test(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = <!DEBUG_INFO_SMARTCAST{OI}!>a<!>[0] // no SMARTCAST diagnostic
        if (b != null) {
            <!DEBUG_INFO_SMARTCAST!>b<!>.inc()
        }
    }
}
