//KT-4332 when/smartcast underperforms

fun testWhen(t: String?, x: String?): Int {
    return when {
        t == null -> 0
        x == null -> <!DEBUG_INFO_SMARTCAST!>t<!>.length // Wrong error report here. t can be inferred as not-null. (And it actually does if you replace when with if/else if)
        else -> (<!DEBUG_INFO_SMARTCAST!>t<!> + x).length
    }
}
