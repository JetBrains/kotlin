fun simpleAnd(): Boolean {
    val lst = arrayListOf(1, 2, 3)
    val b1 = lst.size() > 2 && lst[2] > 0
    val b2 = lst.size() > 3 && <!UNREACHABLE_CODE!>lst[3] > 0<!>    // no alarm, because the first arg is `false`
    return b1 && b2
}