// KT-74654
// KT-90199
annotation class Ann(val s: String)

const val x = "str"

class C {
    val (a, @Ann(<caret>x) b) = 1 to 2
}
