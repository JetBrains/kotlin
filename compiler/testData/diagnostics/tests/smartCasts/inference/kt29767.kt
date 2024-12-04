// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-29767

fun test(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = a[0] // no SMARTCAST diagnostic
        if (b != null) {
            <!DEBUG_INFO_SMARTCAST!>b<!>.inc()
        }
    }
}
