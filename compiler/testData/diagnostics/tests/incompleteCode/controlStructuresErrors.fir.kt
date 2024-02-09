
fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (<!UNRESOLVED_REFERENCE!>rr<!>) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (<!UNRESOLVED_REFERENCE!>l<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.q()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.w()
        }<!>
    }
    else {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (<!UNRESOLVED_REFERENCE!>n<!>) {
            <!UNRESOLVED_REFERENCE!>a<!>.t()
        }
        else {
            <!UNRESOLVED_REFERENCE!>a<!>.u()
        }<!>
    }<!>
}

fun test2(l: List<<!UNRESOLVED_REFERENCE!>AA<!>>) {
    l.<!UNRESOLVED_REFERENCE!>map<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        <!UNRESOLVED_REFERENCE!>it<!>!!
    }<!>
}
