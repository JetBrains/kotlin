fun calc(x: List<String>?): Int {
    // x is not-null only inside subList
    x?.subList(0, x.size - 1).<!INAPPLICABLE_CANDIDATE!>get<!>(x.<!INAPPLICABLE_CANDIDATE!>size<!>)
    return x!!.size
}
