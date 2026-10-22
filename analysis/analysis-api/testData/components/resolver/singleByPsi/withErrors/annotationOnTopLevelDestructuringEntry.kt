// KT-74654
// KT-90199
annotation class Ann(val s: String)

const val x = "str"

val (a: Int, @A<caret>nn(x) b: Int) = 1 to 2
