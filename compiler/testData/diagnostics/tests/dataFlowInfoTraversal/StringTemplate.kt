// !CHECK_TYPE

fun foo(x: Number, y: String?): String {
    val result = "abcde $x ${x as Int} ${y!!} $x $y"
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
    checkSubtype<String>(<!DEBUG_INFO_SMARTCAST!>y<!>)
    return result
}
