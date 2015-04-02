fun calc(x: List<String>?, y: Int?): Int {
    x?.subList(y!! - 1, <!DEBUG_INFO_SMARTCAST!>y<!>)
    // y!! above should not provide smart cast here
    return <!TYPE_MISMATCH!>y<!>
}
