fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list, including inner call
    x?.get(x.get(x.size - 1).length)
    // but not also here!
    return x.<!INAPPLICABLE_CANDIDATE!>size<!>
}
