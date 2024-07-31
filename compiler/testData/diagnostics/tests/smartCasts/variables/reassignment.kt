// ISSUE: KT-65349

fun test(a1: String?) {

    val a2: String? = a1
    if (a2 != null) {
        <!DEBUG_INFO_SMARTCAST!>a1<!>.length
    }
    if (a1 != null) {
        a2<!UNSAFE_CALL!>.<!>length
    }

    val a3 = a1
    if (a3 != null) {
        <!DEBUG_INFO_SMARTCAST!>a1<!>.length
    }
    if (a1 != null) {
        a3<!UNSAFE_CALL!>.<!>length
    }
}

fun test2(a1: Any) {

    val a2: Any = a1
    if (a2 is String) {
        a1.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (a1 is String) {
        a2.<!UNRESOLVED_REFERENCE!>length<!>
    }

    val a3 = a1
    if (a3 is String) {
        a1.<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (a1 is String) {
        a3.<!UNRESOLVED_REFERENCE!>length<!>
    }
}