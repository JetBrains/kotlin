fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // See KT-6283
    do {
        p!!.length
    } while (!x())
    // Do-while loop is executed at least once, so
    // p should be not null here
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}