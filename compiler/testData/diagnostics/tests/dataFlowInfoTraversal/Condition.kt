fun foo(x: Int?): Boolean {
    val result = ((x!! == 0) && ((<!DEBUG_INFO_AUTOCAST!>x<!> : Int) == 0))
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    return result
}
