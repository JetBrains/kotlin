fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list, including inner call
    x?.get(<!DEBUG_INFO_SMARTCAST!>x<!>.get(<!DEBUG_INFO_SMARTCAST!>x<!>.size - 1).length)
    // but not also here!
    return x<!UNSAFE_CALL!>.<!>size
}
