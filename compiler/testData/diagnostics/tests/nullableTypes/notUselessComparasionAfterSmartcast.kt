// ISSUE: KT-53460

fun test(a: String?, b: String?) {
    if (a == null || a == "foo") {
        when (b) {
            "abc" -> return
        }
    }

    <!DEBUG_INFO_SMARTCAST!>a<!>.length // should be an error
    if (<!SENSELESS_COMPARISON!>a == null<!> || a == "bar") { // comparasion to null is not useless
        <!DEBUG_INFO_SMARTCAST!>a<!>.length
    }
}
