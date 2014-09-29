fun foo(x: Number, y: String?): String {
    val result = "abcde $x ${x as Int} ${y!!} $x $y"
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    <!DEBUG_INFO_SMARTCAST!>y<!> : String
    return result
}
