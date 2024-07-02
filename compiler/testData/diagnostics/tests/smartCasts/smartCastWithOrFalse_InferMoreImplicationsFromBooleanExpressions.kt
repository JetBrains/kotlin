// LANGUAGE: +InferMoreImplicationsFromBooleanExpressions

fun main(x: Any?) {
    if (x is String || false) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else if (false || x is String) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
