// !WITH_NEW_INFERENCE
// ISSUE: KT-29767

fun test(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = <!OI;DEBUG_INFO_SMARTCAST!>a<!>[0] // no SMARTCAST diagnostic
        if (b != null) {
            <!DEBUG_INFO_SMARTCAST!>b<!>.inc()
        }
    }
}