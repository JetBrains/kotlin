fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list
    x?.get(x.size - 1)
    // but not also here!
    return x.<!INAPPLICABLE_CANDIDATE!>size<!>
}
