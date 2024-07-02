// LANGUAGE: +InferMoreImplicationsFromBooleanExpressions

fun main(x: Any?) {
    if (x is String || false) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else if (false || x is String) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
