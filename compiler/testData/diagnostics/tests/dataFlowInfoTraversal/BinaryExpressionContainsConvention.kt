fun foo(x: Number): Boolean {
    val result = (x as Int) in 1..5
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    return result
}
