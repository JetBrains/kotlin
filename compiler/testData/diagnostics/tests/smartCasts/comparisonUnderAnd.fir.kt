// !WITH_NEW_INFERENCE
fun foo(x : String?, y : String?) {
    if (y != null && x == y) {
        // Both not null
        x.length
        y.length
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    if (y != null || x == y) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    else {
        // y == null but x != y
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    if (y == null && x != y) {
        // y == null but x != y
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    if (y == null || x != y) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!>
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
    else {
        // Both not null
        x.length
        y.length
    }
}