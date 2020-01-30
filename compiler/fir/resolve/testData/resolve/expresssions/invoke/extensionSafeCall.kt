fun bar(doIt: Int.() -> Int) {
    val i: Int? = 1
    i?.<!INAPPLICABLE_CANDIDATE!>doIt<!>()
}
