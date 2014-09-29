fun Int.component1() = "a"

fun foo(a: Number) {
    val (x) = a as Int
    <!DEBUG_INFO_SMARTCAST!>a<!> : Int
    x : String
}
