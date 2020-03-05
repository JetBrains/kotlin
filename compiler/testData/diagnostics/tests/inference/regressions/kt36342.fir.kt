// !WITH_NEW_INFERENCE

import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)!!
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!!!
    try {
        <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } catch (e: Exception) {
        <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }

    if (true)
        <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    else
        <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)

    when {
        true -> <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>) ?: <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
}
