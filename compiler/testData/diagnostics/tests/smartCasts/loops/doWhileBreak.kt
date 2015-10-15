fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // See KT-6283
    do {
        p!!.length
        if (p == "abc") break
    } while (!x())
    // p should be smart casted despite of break
    return <!DEBUG_INFO_SMARTCAST!>p<!>.length
}