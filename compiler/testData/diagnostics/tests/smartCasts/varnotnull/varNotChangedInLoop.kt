public fun foo() {
    var i: Int? = 1
    if (i != null) {
        while (i != 10) {
            <!DEBUG_INFO_SMARTCAST!>i<!>++
        }
    }
}