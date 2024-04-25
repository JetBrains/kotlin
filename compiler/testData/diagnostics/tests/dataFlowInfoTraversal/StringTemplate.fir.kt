// CHECK_TYPE

fun foo(x: Number, y: String?): String {
    val result = "abcde $x ${x as Int} ${y!!} $x $y"
    checkSubtype<Int>(x)
    checkSubtype<String>(y)
    return result
}
