fun foo(y: Int) = y

fun calc(x: List<String>?): Int {
    foo(x!!.size)
    // Here we should have smart cast because of x!!, despite of KT-7204 fixed
    return <!DEBUG_INFO_SMARTCAST!>x<!>.size
}
