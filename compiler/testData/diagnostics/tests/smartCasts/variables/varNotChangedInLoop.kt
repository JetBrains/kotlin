public fun foo() {
    var i: Any = 1
    if (i is Int) {
        while (i != 10) {
            <!DEBUG_INFO_SMARTCAST!>i<!>++
        }
    }
}