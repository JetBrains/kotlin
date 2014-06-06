//KT-4332 when/autocast underperforms

fun testWhen(t: String?, x: String?): Int {
    return when {
        t == null -> 0
        x == null -> <!DEBUG_INFO_AUTOCAST!>t<!>.length // Wrong error report here. t can be inferred as not-null. (And it actually does if you replace when with if/else if)
        else -> (<!DEBUG_INFO_AUTOCAST!>t<!> + x).length
    }
}