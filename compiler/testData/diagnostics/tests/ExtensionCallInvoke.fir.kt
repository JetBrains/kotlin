fun bar(doIt: Int.() -> Int) {
    1.doIt()
    1<!UNNECESSARY_SAFE_CALL!>?.<!>doIt()
    val i: Int? = 1
    <!ARGUMENT_TYPE_MISMATCH!>i<!>.doIt()
    i?.doIt()
}
