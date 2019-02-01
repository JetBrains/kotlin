// !WITH_NEW_INFERENCE

class Data<T>(val s: T)

fun test_1(d: Data<out Any>) {
    if (d.s is String) {
        <!OI;DEBUG_INFO_SMARTCAST!>d.s<!>.<!NI;UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test_2(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = <!OI;DEBUG_INFO_SMARTCAST!>a<!>[0] // no SMARTCAST diagnostic
        if (b != null) {
            <!DEBUG_INFO_SMARTCAST!>b<!>.inc()
        }
    }
}