// !WITH_NEW_INFERENCE

import java.lang.Exception

fun <K> id(arg: K): K = arg

fun test() {
    id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)!!
    <!UNRESOLVED_REFERENCE!>unresolved<!>!!!!
    try {
        id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
    } catch (e: Exception) {
        id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
    }

    if (true)
        id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
    else
        id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)

    when {
        true -> id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
    }
    id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>) ?: id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
}
