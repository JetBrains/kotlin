fun calc(x: List<String>?): Int {
    // x is not-null only inside subList
    x?.subList(0, <!DEBUG_INFO_SMARTCAST!>x<!>.size - 1)<!UNSAFE_CALL!>.<!>get(x<!UNSAFE_CALL!>.<!>size)
    return x!!.size
}
