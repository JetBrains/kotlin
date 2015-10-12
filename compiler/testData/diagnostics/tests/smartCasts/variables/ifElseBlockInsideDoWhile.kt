public fun foo(xx: Any): Int {
    var x = xx
    do {
        var y: Any
        // After the check, smart cast should work
        if (x is String) {
            y = "xyz"
        } else {
            y = "abc"
        }
        // y!! in both branches
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    } while (!(x is String))
    return <!DEBUG_INFO_SMARTCAST!>x<!>.length
}