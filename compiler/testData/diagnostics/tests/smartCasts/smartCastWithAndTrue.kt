fun main(x: Any?) {
    if (x is String && true) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else if (true && x is String) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
