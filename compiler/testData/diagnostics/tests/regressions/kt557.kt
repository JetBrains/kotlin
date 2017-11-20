// !WITH_NEW_INFERENCE
// KT-557 Wrong type inference near sure extension function

fun <T : Any> T?.sure() : T = this!!

fun Array<String>.length() : Int {
    return 0;
}

fun test(array : Array<String?>?) {
    <!TYPE_MISMATCH!>array?.sure<Array<String?>>()<!>.length()
}
