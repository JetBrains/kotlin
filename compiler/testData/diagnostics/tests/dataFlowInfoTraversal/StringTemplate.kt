fun foo(x: Number, y: String?): String {
    val result = "abcde $x ${x as Int} ${y!!} $x $y"
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
    <!DEBUG_INFO_AUTOCAST!>y<!> : String
    return result
}
