fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list
    x?.subList(x.size - 1, x.size)
    // but not also here!
    return x<!UNSAFE_CALL!>.<!>size
}
