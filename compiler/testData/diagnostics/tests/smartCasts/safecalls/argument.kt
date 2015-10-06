fun calc(x: List<String>?): Int {
    // After KT-5840 fix !! assertion should become unnecessary here
    x?.get(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.size - 1)
    // x?. or x!! above should not provide smart cast here
    return x<!UNSAFE_CALL!>.<!>size
}
