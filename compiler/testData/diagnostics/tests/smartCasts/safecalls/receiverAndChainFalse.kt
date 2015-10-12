fun foo(x: String): String? = x

fun calc(x: String?, y: Int?): Int {
    // Smart cast because of x!! in receiver
    foo(x!!)?.subSequence(y!!, <!DEBUG_INFO_SMARTCAST!>x<!>.length)?.length
    // No smart cast possible
    return <!TYPE_MISMATCH!>y<!>    
}
