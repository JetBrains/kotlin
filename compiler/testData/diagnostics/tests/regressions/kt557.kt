// KT-557 Wrong type inference near sure extension function

fun Array<String>.length() : Int {
    return 0;
}

fun test(array : Array<String?>?) {
    array?.sure<Array<String?>>()<!UNSAFE_CALL!>.<!>length()
}
