// !WITH_NEW_INFERENCE
// ISSUE: KT-25432

class Data<T>(val s: T)

fun test(d: Data<out Any>) {
    if (d.s is String) {
        <!OI;DEBUG_INFO_SMARTCAST!>d.s<!>.<!NI;UNRESOLVED_REFERENCE!>length<!>
    }
}

