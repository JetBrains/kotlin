fun foo(x: String): String? = x

fun calc(x: String?, y: String?): Int {
    // Smart cast because of y!! in receiver
    x?.subSequence(y!!.subSequence(0, 1).length, <!DEBUG_INFO_SMARTCAST!>y<!>.length)
    // No smart cast possible
    return y<!UNSAFE_CALL!>.<!>length
}
