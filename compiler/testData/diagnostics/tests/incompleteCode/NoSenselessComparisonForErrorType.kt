// !WITH_NEW_INFERENCE
package a

fun foo() {
    val a = <!UNRESOLVED_REFERENCE!>getErrorType<!>()
    if (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> null) { //no senseless comparison

    }
}