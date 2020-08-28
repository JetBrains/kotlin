fun bar(doIt: Int.() -> Int) {
    1.doIt()
    1?.doIt()
    val i: Int? = 1
    i.<!INAPPLICABLE_CANDIDATE!>doIt<!>()
    i?.doIt()
}
