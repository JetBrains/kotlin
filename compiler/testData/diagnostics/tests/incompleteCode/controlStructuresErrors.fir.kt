
fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (<!UNRESOLVED_REFERENCE!>rr<!>) {
        if (<!UNRESOLVED_REFERENCE!>l<!>) {
            <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>a<!>.q()<!>
        }
        else {
            <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>a<!>.w()<!>
        }
    }
    else {
        if (<!UNRESOLVED_REFERENCE!>n<!>) {
            <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>a<!>.t()<!>
        }
        else {
            <!ARGUMENT_TYPE_MISMATCH!><!UNRESOLVED_REFERENCE!>a<!>.u()<!>
        }
    }<!>
}

fun test2(l: List<<!UNRESOLVED_REFERENCE!>AA<!>>) {
    l.<!UNRESOLVED_REFERENCE!>map<!> {
        <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>it<!>!!
    }
}
