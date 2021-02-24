// !WITH_NEW_INFERENCE
// KT-557 Wrong type inference near sure extension function

fun <T : Any> T?.sure() : T = this!!

fun Array<String>.length() : Int {
    return 0;
}

fun test(array : Array<String?>?) {
    <!TYPE_MISMATCH{OI}!>array?.sure<Array<String?>>()<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>length<!>()
}
