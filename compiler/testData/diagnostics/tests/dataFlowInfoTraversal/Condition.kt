fun foo(x: Int?): Boolean {
    val result = ((x!! == 0) && ((<!DEBUG_INFO_SMARTCAST!>x<!> : Int) == 0))
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    return result
}
