public fun foo(xx: Any): Int {
    var x = xx
    do {
        var y: Any
        // After the check, smart cast should work
        if (x is String) {
            break
        } else {
            y = "abc"
        }
        // y!! in both branches
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    } while (true)
    // We could have smart cast here but with break it's hard to detect
    return x.<!UNRESOLVED_REFERENCE!>length<!>()
}