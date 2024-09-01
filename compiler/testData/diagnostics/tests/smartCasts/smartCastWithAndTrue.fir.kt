fun main(x: Any?) {
    if (x is String && true) {
        x.length
    }
    else if (true && x is String) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
