fun foo(x: Number, y: String?): String {
    val result = "abcde $x ${x as Int} ${y!!} $x $y"
    x : Int
    y : String
    return result
}
